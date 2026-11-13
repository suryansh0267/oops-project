package com.itsm.service;

import com.itsm.dto.*;
import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.*;
import com.itsm.strategy.AssignmentStrategy;
import com.itsm.strategy.PriorityCalculationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTests {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ITServiceRepository itServiceRepository;
    @Mock
    private SLAPolicyRepository slaPolicyRepository;
    @Mock
    private TechnicianProfileRepository technicianProfileRepository;
    @Mock
    private IncidentNoteRepository incidentNoteRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private PriorityCalculationStrategy priorityStrategy;
    @Mock
    private com.itsm.strategy.AssignmentStrategyManager assignmentStrategyManager;

    private IncidentService incidentService;

    private User employee;
    private User technician;
    private ITService itService;
    private SLAPolicy slaPolicy;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                incidentRepository, userRepository, itServiceRepository, slaPolicyRepository,
                technicianProfileRepository, incidentNoteRepository, notificationService,
                auditService, priorityStrategy, assignmentStrategyManager
        );

        employee = User.builder().id(1L).username("emp").fullName("Emp User").role(UserRole.EMPLOYEE).build();
        technician = User.builder().id(2L).username("tech").fullName("Tech User").role(UserRole.TECHNICIAN).build();
        itService = ITService.builder().id(10L).name("Payment Gateway").businessCriticality(5).build();
        slaPolicy = SLAPolicy.builder().id(20L).priority(Priority.P1_CRITICAL).responseTimeMinutes(15).resolutionTimeMinutes(60).build();
    }

    @Test
    @DisplayName("Test 9: Incident Creation with SLA Deadlines & Classification")
    void testCreateIncident() {
        IncidentRequest request = new IncidentRequest();
        request.setTitle("System Down");
        request.setDescription("Database down");
        request.setCategory(IncidentCategory.DATABASE);
        request.setImpact(Impact.HIGH);
        request.setUrgency(Urgency.HIGH);
        request.setAffectedServiceId(10L);

        when(userRepository.findByUsername("emp")).thenReturn(Optional.of(employee));
        when(itServiceRepository.findById(10L)).thenReturn(Optional.of(itService));
        when(priorityStrategy.calculatePriority(Impact.HIGH, Urgency.HIGH, 5)).thenReturn(Priority.P1_CRITICAL);
        when(slaPolicyRepository.findByPriority(Priority.P1_CRITICAL)).thenReturn(Optional.of(slaPolicy));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> {
            Incident inc = i.getArgument(0);
            inc.setId(100L);
            inc.setIncidentNumber("INC1001");
            return inc;
        });

        IncidentResponse response = incidentService.createIncident(request, "emp");

        assertNotNull(response);
        assertEquals("INC1001", response.getIncidentNumber());
        assertEquals("CLASSIFIED", response.getStatus());
        assertEquals("P1_CRITICAL", response.getPriority());
        verify(notificationService, times(1)).notifyIncidentCreated(any(Incident.class));
    }

    @Test
    @DisplayName("Test 10: Invalid State Transition from CLASSIFIED directly to RESOLVED throws Exception")
    void testInvalidStateTransition() {
        Incident incident = Incident.builder()
                .id(100L)
                .status(IncidentStatus.CLASSIFIED)
                .build();

        when(incidentRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(technician));

        ResolveRequest resolveRequest = new ResolveRequest();
        resolveRequest.setResolutionNotes("Quick fix");

        assertThrows(InvalidStateTransitionException.class, () ->
                incidentService.resolveIncident(100L, resolveRequest, "tech"));
    }

    @Test
    @DisplayName("Test 11: Technician Acknowledgment Updates Timestamp and Status")
    void testAcknowledgeIncident() {
        Incident incident = Incident.builder()
                .id(101L)
                .status(IncidentStatus.ASSIGNED)
                .assignedTechnician(technician)
                .build();

        when(incidentRepository.findById(101L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(technician));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        IncidentResponse res = incidentService.acknowledgeIncident(101L, "tech");

        assertEquals("ACKNOWLEDGED", res.getStatus());
        assertNotNull(res.getRespondedAt());
    }

    @Test
    @DisplayName("Test 12: Resolving Incident Updates Workload and Timestamps")
    void testResolveIncident() {
        Incident incident = Incident.builder()
                .id(102L)
                .status(IncidentStatus.IN_PROGRESS)
                .assignedTechnician(technician)
                .build();

        TechnicianProfile profile = TechnicianProfile.builder()
                .user(technician)
                .currentActiveIncidents(2)
                .totalResolvedIncidents(5)
                .build();

        when(incidentRepository.findById(102L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(technician));
        when(technicianProfileRepository.findByUser(technician)).thenReturn(Optional.of(profile));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        ResolveRequest req = new ResolveRequest();
        req.setResolutionNotes("Patched application code");

        IncidentResponse res = incidentService.resolveIncident(102L, req, "tech");

        assertEquals("RESOLVED", res.getStatus());
        assertEquals(1, profile.getCurrentActiveIncidents());
        assertEquals(6, profile.getTotalResolvedIncidents());
    }

    @Test
    @DisplayName("Test 13: Closing Incident sets Closed Status and ClosedAt")
    void testCloseIncident() {
        Incident incident = Incident.builder()
                .id(103L)
                .status(IncidentStatus.RESOLVED)
                .build();

        when(incidentRepository.findById(103L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("emp")).thenReturn(Optional.of(employee));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        IncidentResponse res = incidentService.closeIncident(103L, "emp");
        assertEquals("CLOSED", res.getStatus());
        assertNotNull(res.getClosedAt());
    }

    @Test
    @DisplayName("Test 14: Duplicate Detection Service - Jaccard Word Similarity")
    void testDuplicateDetection_PositiveMatch() {
        DuplicateDetectionService dupService = new DuplicateDetectionService(incidentRepository);

        Incident inc1 = Incident.builder()
                .id(1L)
                .title("Payment application unavailable during checkout")
                .description("Users receiving 503 error on payment submit")
                .status(IncidentStatus.IN_PROGRESS)
                .category(IncidentCategory.APPLICATION)
                .build();

        Incident inc2 = Incident.builder()
                .id(2L)
                .title("Payment application down and unavailable")
                .description("Users receiving 503 error")
                .status(IncidentStatus.NEW)
                .category(IncidentCategory.APPLICATION)
                .build();

        when(incidentRepository.findAll()).thenReturn(List.of(inc1));

        List<Incident> duplicates = dupService.findPotentialDuplicates(inc2);
        assertFalse(duplicates.isEmpty());
        assertEquals(1L, duplicates.get(0).getId());
    }

    @Test
    @DisplayName("Test 15: Duplicate Detection Service - Completely Distinct Incidents")
    void testDuplicateDetection_NoMatch() {
        DuplicateDetectionService dupService = new DuplicateDetectionService(incidentRepository);

        Incident existing = Incident.builder()
                .id(1L)
                .title("Printer paper jam on 3rd floor")
                .description("Hardware mechanical issue")
                .status(IncidentStatus.IN_PROGRESS)
                .category(IncidentCategory.HARDWARE)
                .build();

        Incident newInc = Incident.builder()
                .id(2L)
                .title("Database connection pool exhausted in production")
                .description("Postgres max connections exceeded")
                .status(IncidentStatus.NEW)
                .category(IncidentCategory.DATABASE)
                .build();

        when(incidentRepository.findAll()).thenReturn(List.of(existing));

        List<Incident> duplicates = dupService.findPotentialDuplicates(newInc);
        assertTrue(duplicates.isEmpty());
    }

    @Test
    @DisplayName("Test 16: Adding Feedback and Rating to Incident")
    void testAddFeedback() {
        Incident incident = Incident.builder()
                .id(104L)
                .status(IncidentStatus.CLOSED)
                .build();

        when(incidentRepository.findById(104L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        FeedbackRequest req = new FeedbackRequest();
        req.setFeedback("Great support, fixed quickly!");
        req.setRating(5);

        IncidentResponse res = incidentService.addFeedback(104L, req, "emp");
        assertEquals("Great support, fixed quickly!", res.getFeedback());
        assertEquals(5, res.getFeedbackRating());
    }

    @Test
    @DisplayName("Test 17: ResourceNotFoundException thrown on invalid Incident ID")
    void testResourceNotFound() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> incidentService.getIncidentById(999L));
    }
}
