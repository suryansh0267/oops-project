package com.itsm.service;

import com.itsm.dto.*;
import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventType;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.TechnicianUnavailableException;
import com.itsm.exception.WorkloadExceededException;
import com.itsm.repository.*;
import com.itsm.security.IncidentAccessPolicy;
import com.itsm.strategy.AssignmentStrategyManager;
import com.itsm.strategy.PriorityStrategyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTests {

    @Mock private IncidentRepository incidentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ITServiceRepository itServiceRepository;
    @Mock private SLAPolicyRepository slaPolicyRepository;
    @Mock private TechnicianProfileRepository technicianProfileRepository;
    @Mock private IncidentNoteRepository incidentNoteRepository;
    @Mock private AuditService auditService;
    @Mock private PriorityStrategyManager priorityStrategyManager;
    @Mock private AssignmentStrategyManager assignmentStrategyManager;
    @Mock private IncidentCategoryService categoryService;
    @Mock private DuplicateDetectionService duplicateDetectionService;
    @Mock private ApplicationEventPublisher events;

    private IncidentService incidentService;

    private User employee;
    private User otherEmployee;
    private User technician;
    private User otherTechnician;
    private User admin;
    private ITService itService;
    private SLAPolicy slaPolicy;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository, userRepository, itServiceRepository,
                slaPolicyRepository, technicianProfileRepository, incidentNoteRepository, auditService,
                priorityStrategyManager, assignmentStrategyManager, categoryService, duplicateDetectionService,
                new IncidentAccessPolicy(), new IncidentMapper(), events);

        employee = User.builder().id(1L).username("emp").fullName("Emp User").role(UserRole.EMPLOYEE).active(true).build();
        otherEmployee = User.builder().id(6L).username("emp2").fullName("Other Emp").role(UserRole.EMPLOYEE).active(true).build();
        technician = User.builder().id(2L).username("tech").fullName("Tech User").role(UserRole.TECHNICIAN).active(true).build();
        otherTechnician = User.builder().id(3L).username("tech2").fullName("Tech Two").role(UserRole.TECHNICIAN).active(true).build();
        admin = User.builder().id(9L).username("admin").fullName("Admin").role(UserRole.ADMIN).active(true).build();
        itService = ITService.builder().id(10L).name("Payment Gateway").businessCriticality(5).active(true).build();
        slaPolicy = SLAPolicy.builder().id(20L).priority(Priority.P1_CRITICAL)
                .responseTimeMinutes(15).resolutionTimeMinutes(60).build();
    }

    private Incident incident(long id, IncidentStatus status, User tech) {
        return Incident.builder().id(id).incidentNumber(String.format("INC%05d", id)).title("Test")
                .reporter(employee).assignedTechnician(tech).status(status).priority(Priority.P2_HIGH)
                .createdAt(LocalDateTime.now().minusMinutes(30)).build();
    }

    private TechnicianProfile profile(User user, int active, int max, boolean available) {
        return TechnicianProfile.builder().user(user).currentActiveIncidents(active)
                .maxActiveIncidents(max).available(available).build();
    }

    // ------------------------------------------------------------------ creation

    @Test
    @DisplayName("Create: priority, SLA deadlines, unique incident number and CREATED event")
    void createIncident() {
        IncidentRequest request = new IncidentRequest();
        request.setTitle("Payment gateway timeout");
        request.setDescription("Card payments time out");
        request.setCategory("database");
        request.setImpact(Impact.HIGH);
        request.setUrgency(Urgency.HIGH);
        request.setAffectedServiceId(10L);

        when(userRepository.findByUsername("emp")).thenReturn(Optional.of(employee));
        when(itServiceRepository.findById(10L)).thenReturn(Optional.of(itService));
        when(categoryService.requireActive("database")).thenReturn("DATABASE");
        when(priorityStrategyManager.calculate(Impact.HIGH, Urgency.HIGH, 5, null)).thenReturn(Priority.P1_CRITICAL);
        when(priorityStrategyManager.getActiveStrategyKey()).thenReturn("defaultPriority");
        when(slaPolicyRepository.findByPriority(Priority.P1_CRITICAL)).thenReturn(Optional.of(slaPolicy));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            if (i.getId() == null) i.setId(42L);
            return i;
        });
        when(duplicateDetectionService.findPotentialDuplicates(any())).thenReturn(List.of());

        IncidentResponse res = incidentService.createIncident(request, "emp");

        assertEquals("INC00042", res.getIncidentNumber());
        assertEquals("P1_CRITICAL", res.getPriority());
        assertEquals("CLASSIFIED", res.getStatus());
        assertEquals("DATABASE", res.getCategory());
        assertNotNull(res.getResponseDeadline());
        assertTrue(res.getResolutionDeadline().isAfter(res.getResponseDeadline()));
        verify(events).publishEvent(argThat((IncidentEvent e) -> e.type() == IncidentEventType.CREATED));
    }

    @Test
    @DisplayName("Create: blank category is filled in by the automatic categoriser")
    void createAutoCategorises() {
        IncidentRequest request = new IncidentRequest();
        request.setTitle("VPN keeps dropping");
        request.setDescription("wifi and vpn unstable");

        when(userRepository.findByUsername("emp")).thenReturn(Optional.of(employee));
        when(categoryService.suggest(anyString(), anyString()))
                .thenReturn(new com.itsm.intelligence.CategoryClassifier.Suggestion("NETWORK", 0.8, List.of("vpn")));
        when(priorityStrategyManager.calculate(Impact.MEDIUM, Urgency.MEDIUM, 2, null)).thenReturn(Priority.P3_MEDIUM);
        when(priorityStrategyManager.getActiveStrategyKey()).thenReturn("defaultPriority");
        when(slaPolicyRepository.findByPriority(Priority.P3_MEDIUM)).thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            if (i.getId() == null) i.setId(7L);
            return i;
        });
        when(duplicateDetectionService.findPotentialDuplicates(any())).thenReturn(List.of());

        IncidentResponse res = incidentService.createIncident(request, "emp");
        assertEquals("NETWORK", res.getCategory());
        verify(categoryService, never()).requireActive(any());
    }

    // ------------------------------------------------------------------ lifecycle

    @Test
    @DisplayName("Invalid transition CLASSIFIED -> RESOLVED throws InvalidStateTransitionException")
    void invalidStateTransition() {
        when(incidentRepository.findById(100L)).thenReturn(Optional.of(incident(100L, IncidentStatus.CLASSIFIED, null)));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        ResolveRequest req = new ResolveRequest();
        req.setResolutionNotes("Restarted the service");
        assertThrows(InvalidStateTransitionException.class, () -> incidentService.resolveIncident(100L, req, "admin"));
    }

    @Test
    @DisplayName("Acknowledge by the assignee records the first response time")
    void acknowledgeIncident() {
        Incident inc = incident(101L, IncidentStatus.ASSIGNED, technician);
        when(incidentRepository.findById(101L)).thenReturn(Optional.of(inc));
        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(technician));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        IncidentResponse res = incidentService.acknowledgeIncident(101L, "tech");
        assertEquals("ACKNOWLEDGED", res.getStatus());
        assertNotNull(inc.getRespondedAt());
    }

    @Test
    @DisplayName("A technician cannot acknowledge someone else's incident (unauthorised operation)")
    void acknowledgeByOtherTechnicianDenied() {
        when(incidentRepository.findById(102L)).thenReturn(Optional.of(incident(102L, IncidentStatus.ASSIGNED, technician)));
        when(userRepository.findByUsername("tech2")).thenReturn(Optional.of(otherTechnician));
        assertThrows(AccessDeniedException.class, () -> incidentService.acknowledgeIncident(102L, "tech2"));
    }

    @Test
    @DisplayName("Resolve frees technician capacity and updates resolved count and average time")
    void resolveIncident() {
        Incident inc = incident(103L, IncidentStatus.IN_PROGRESS, technician);
        TechnicianProfile p = profile(technician, 3, 10, true);
        p.setTotalResolvedIncidents(4);
        when(incidentRepository.findById(103L)).thenReturn(Optional.of(inc));
        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(technician));
        when(technicianProfileRepository.findByUser(technician)).thenReturn(Optional.of(p));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        ResolveRequest req = new ResolveRequest();
        req.setResolutionNotes("Rolled back the faulty release");
        IncidentResponse res = incidentService.resolveIncident(103L, req, "tech");

        assertEquals("RESOLVED", res.getStatus());
        assertEquals(2, p.getCurrentActiveIncidents());
        assertEquals(5, p.getTotalResolvedIncidents());
        assertTrue(p.getAvgResolutionTimeMinutes() > 0);
    }

    @Test
    @DisplayName("Close records the real previous state in the audit trail")
    void closeAuditsPreviousState() {
        when(incidentRepository.findById(104L)).thenReturn(Optional.of(incident(104L, IncidentStatus.RESOLVED, technician)));
        when(userRepository.findByUsername("emp")).thenReturn(Optional.of(employee));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        IncidentResponse res = incidentService.closeIncident(104L, "emp");
        assertEquals("CLOSED", res.getStatus());
        assertNotNull(res.getClosedAt());
        verify(auditService).logAction(any(), eq(employee), eq("CLOSED"), eq("RESOLVED"), eq("CLOSED"), anyString());
    }

    @Test
    @DisplayName("Reporter can reopen a resolved incident; it returns to the technician")
    void reopen() {
        Incident inc = incident(105L, IncidentStatus.RESOLVED, technician);
        when(incidentRepository.findById(105L)).thenReturn(Optional.of(inc));
        when(userRepository.findByUsername("emp")).thenReturn(Optional.of(employee));
        when(technicianProfileRepository.findByUser(technician)).thenReturn(Optional.of(profile(technician, 0, 5, true)));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("IN_PROGRESS", incidentService.reopenIncident(105L, "Still failing", "emp").getStatus());
    }

    // ------------------------------------------------------------------ assignment rules

    @Test
    @DisplayName("Reassignment moves workload from the old technician to the new one")
    void reassignmentMovesWorkload() {
        Incident inc = incident(106L, IncidentStatus.ASSIGNED, technician);
        TechnicianProfile oldP = profile(technician, 4, 10, true);
        TechnicianProfile newP = profile(otherTechnician, 1, 10, true);
        when(incidentRepository.findById(106L)).thenReturn(Optional.of(inc));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherTechnician));
        when(technicianProfileRepository.findByUser(technician)).thenReturn(Optional.of(oldP));
        when(technicianProfileRepository.findByUser(otherTechnician)).thenReturn(Optional.of(newP));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        AssignRequest req = new AssignRequest();
        req.setTechnicianId(3L);
        incidentService.assignIncident(106L, req, "admin");

        assertEquals(3, oldP.getCurrentActiveIncidents());
        assertEquals(2, newP.getCurrentActiveIncidents());
        assertEquals(otherTechnician, inc.getAssignedTechnician());
    }

    @Test
    @DisplayName("Assigning to a technician at capacity throws WorkloadExceededException")
    void workloadExceeded() {
        when(incidentRepository.findById(107L)).thenReturn(Optional.of(incident(107L, IncidentStatus.CLASSIFIED, null)));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(technician));
        when(technicianProfileRepository.findByUser(technician)).thenReturn(Optional.of(profile(technician, 5, 5, true)));

        AssignRequest req = new AssignRequest();
        req.setTechnicianId(2L);
        assertThrows(WorkloadExceededException.class, () -> incidentService.assignIncident(107L, req, "admin"));
    }

    @Test
    @DisplayName("Assigning to an unavailable technician throws TechnicianUnavailableException")
    void technicianUnavailable() {
        when(incidentRepository.findById(108L)).thenReturn(Optional.of(incident(108L, IncidentStatus.CLASSIFIED, null)));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(technician));
        when(technicianProfileRepository.findByUser(technician)).thenReturn(Optional.of(profile(technician, 0, 5, false)));

        AssignRequest req = new AssignRequest();
        req.setTechnicianId(2L);
        assertThrows(TechnicianUnavailableException.class, () -> incidentService.assignIncident(108L, req, "admin"));
    }

    // ------------------------------------------------------------------ authorisation & validation

    @Test
    @DisplayName("An employee cannot view another employee's incident")
    void employeeCannotViewOthers() {
        when(incidentRepository.findById(109L)).thenReturn(Optional.of(incident(109L, IncidentStatus.CLASSIFIED, null)));
        when(userRepository.findByUsername("emp2")).thenReturn(Optional.of(otherEmployee));
        assertThrows(AccessDeniedException.class, () -> incidentService.getIncidentById(109L, "emp2"));
    }

    @Test
    @DisplayName("Feedback is rejected while open and accepted once closed")
    void feedbackRules() {
        FeedbackRequest req = new FeedbackRequest();
        req.setFeedback("Great support");
        req.setRating(5);
        when(userRepository.findByUsername("emp")).thenReturn(Optional.of(employee));

        when(incidentRepository.findById(110L)).thenReturn(Optional.of(incident(110L, IncidentStatus.IN_PROGRESS, technician)));
        assertThrows(BadRequestException.class, () -> incidentService.addFeedback(110L, req, "emp"));

        when(incidentRepository.findById(111L)).thenReturn(Optional.of(incident(111L, IncidentStatus.CLOSED, technician)));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));
        IncidentResponse res = incidentService.addFeedback(111L, req, "emp");
        assertEquals(5, res.getFeedbackRating());
    }

    @Test
    @DisplayName("Search rejects a 'from' date after the 'to' date")
    void searchDateValidation() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        LocalDateTime now = LocalDateTime.now();
        assertThrows(BadRequestException.class, () -> incidentService.search(null, null, null, null, null, null,
                null, now, now.minusDays(1), false, "admin"));
    }

    @Test
    @DisplayName("Unknown incident id throws ResourceNotFoundException")
    void resourceNotFound() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> incidentService.getIncidentById(999L, "admin"));
    }

    // ------------------------------------------------------------------ duplicate detection

    @Test
    @DisplayName("Duplicate detection finds the similar open payment incident")
    void duplicateDetectionPositive() {
        DuplicateDetectionService dup = new DuplicateDetectionService(incidentRepository);
        Incident existing = Incident.builder().id(1L).incidentNumber("INC00001")
                .title("Payment application unavailable during checkout")
                .description("Users receiving 503 error on payment submit")
                .status(IncidentStatus.IN_PROGRESS).category("APPLICATION").build();
        Incident fresh = Incident.builder().id(2L).title("Payment application down and unavailable")
                .description("Users receiving 503 error").status(IncidentStatus.CLASSIFIED).category("APPLICATION").build();
        when(incidentRepository.findByStatusIn(any())).thenReturn(List.of(existing, fresh));

        List<Incident> result = dup.findPotentialDuplicates(fresh);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("Duplicate detection ignores unrelated incidents")
    void duplicateDetectionNegative() {
        DuplicateDetectionService dup = new DuplicateDetectionService(incidentRepository);
        Incident printer = Incident.builder().id(1L).title("Printer paper jam on 3rd floor")
                .description("Hardware mechanical issue").status(IncidentStatus.IN_PROGRESS).category("HARDWARE").build();
        Incident db = Incident.builder().id(2L).title("Database connection pool exhausted")
                .description("Postgres max connections exceeded").status(IncidentStatus.CLASSIFIED).category("DATABASE").build();
        when(incidentRepository.findByStatusIn(any())).thenReturn(List.of(printer));

        assertTrue(dup.findPotentialDuplicates(db).isEmpty());
    }
}
