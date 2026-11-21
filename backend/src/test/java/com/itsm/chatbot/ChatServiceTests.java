package com.itsm.chatbot;

import com.itsm.dto.*;
import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.intelligence.CategoryClassifier;
import com.itsm.intelligence.frustration.*;
import com.itsm.repository.*;
import com.itsm.security.IncidentAccessPolicy;
import com.itsm.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Chat workflow unit tests; no database, network or Spring context. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTests {
    @Mock ChatSessionRepository sessions;
    @Mock ChatMessageRepository messages;
    @Mock UserRepository users;
    @Mock IncidentRepository incidents;
    @Mock ITServiceRepository services;
    @Mock IncidentService incidentService;
    @Mock IncidentCategoryService categories;
    @Mock DuplicateDetectionService duplicates;
    @Mock DashboardService dashboard;
    @Mock FrustrationService frustration;
    @Mock ChatEngineRegistry engines;
    @Mock ChatRateLimiter limiter;
    @Mock ApplicationEventPublisher events;

    ChatService chat;
    ChatSession session;
    User employee;

    @BeforeEach void setUp() {
        employee = User.builder().id(1L).username("emp").role(UserRole.EMPLOYEE).build();
        session = new ChatSession(); session.setId(10L); session.setUser(employee);
        when(users.findByUsername("emp")).thenReturn(Optional.of(employee));
        when(sessions.findLockedByUser(employee)).thenReturn(Optional.of(session));
        when(incidents.findByReporterOrderByCreatedAtDesc(employee)).thenReturn(List.of());
        when(frustration.observe(eq(employee), nullable(Incident.class), anyString(), anyBoolean())).thenReturn(
                new FrustrationAssessment(0, FrustrationLevel.CALM, List.of(), "STEADY"));
        when(engines.engine()).thenReturn(new RuleBasedChatEngine());
        when(messages.findTop201BySessionOrderByIdDesc(session)).thenReturn(List.of());
        chat = new ChatService(sessions, messages, users, incidents, services, incidentService, categories,
                duplicates, dashboard, new IncidentAccessPolicy(), frustration, engines, new IntentClassifier(),
                limiter, events, Clock.fixed(Instant.parse("2026-11-21T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test void reportFlowCreatesOnlyAfterConfirmationAndDoubleConfirmIsSafe() {
        ITService vpn = ITService.builder().id(2L).name("VPN Gateway").active(true).build();
        when(services.findAll()).thenReturn(List.of(vpn));
        when(categories.suggest(anyString(), anyString())).thenReturn(
                new CategoryClassifier.Suggestion("NETWORK", .8, List.of("vpn")));
        when(duplicates.findSimilarToDraft(anyString(), anyString(), any(), anyString())).thenReturn(List.of());
        IncidentResponse created = IncidentResponse.builder().id(25L).incidentNumber("INC00025")
                .priority("P3_MEDIUM").statusLabel("Assigned").title("VPN not working").slaStatus("ON_TRACK").build();
        when(incidentService.createIncident(any(IncidentRequest.class), eq("emp"))).thenReturn(created);
        assertTrue(chat.message("emp", "Report an incident").text().contains("Which IT service"));
        assertEquals(ChatState.AWAIT_SERVICE, session.getState());
        chat.message("emp", "VPN Gateway");
        assertEquals(ChatState.AWAIT_DESCRIPTION, session.getState());
        chat.message("emp", "My VPN is not working");
        assertEquals(ChatState.AWAIT_IMPACT, session.getState());
        chat.message("emp", "Just me");
        assertEquals(ChatState.AWAIT_CONFIRM, session.getState());
        assertTrue(chat.message("emp", "Confirm").text().contains("INC00025"));
        assertEquals(ChatState.IDLE, session.getState());
        chat.message("emp", "Confirm");
        verify(incidentService, times(1)).createIncident(any(IncidentRequest.class), eq("emp"));
    }

    @Test void cancelNeverCallsIncidentCreation() {
        when(services.findAll()).thenReturn(List.of());
        chat.message("emp", "Report an incident");
        assertEquals(ChatState.AWAIT_SERVICE, session.getState());
        assertTrue(chat.message("emp", "Cancel").text().contains("cancelled"));
        assertEquals(ChatState.IDLE, session.getState());
        verifyNoInteractions(incidentService);
    }

    @Test void employeeCannotLearnAnythingAboutAnotherReporterIncident() {
        User other = User.builder().id(99L).username("other").role(UserRole.EMPLOYEE).build();
        Incident privateIncident = Incident.builder().id(42L).incidentNumber("INC00042")
                .title("Private Payroll Breach").reporter(other).build();
        when(incidents.findByIncidentNumberIgnoreCase("INC00042")).thenReturn(Optional.of(privateIncident));
        String response = chat.message("emp", "status of INC00042").text();
        assertTrue(response.contains("can't access"));
        assertFalse(response.contains("Payroll"));
        verify(incidentService, never()).getIncidentById(eq(42L), anyString());
    }

    @Test void handoffNeedsExplicitConfirmation() {
        Incident own = Incident.builder().id(4L).incidentNumber("INC00004").reporter(employee)
                .status(IncidentStatus.IN_PROGRESS).build();
        session.setActiveIncidentId(4L);
        when(incidents.findById(4L)).thenReturn(Optional.of(own));
        assertTrue(chat.message("emp", "speak to a manager").text().contains("Confirm"));
        verify(incidentService, never()).addNote(anyLong(), any(), anyString());
        when(incidentService.getIncidentById(4L, "emp")).thenReturn(IncidentResponse.builder().slaStatus("ON_TRACK").build());
        assertTrue(chat.message("emp", "Confirm handoff").text().contains("Human follow-up requested"));
        verify(incidentService, times(1)).addNote(eq(4L), any(NoteRequest.class), eq("emp"));
        assertEquals(ChatState.IDLE, session.getState());
    }

    @Test void employeeHistoryHasNoFrustrationMetadata() {
        when(sessions.findByUser(employee)).thenReturn(Optional.of(session));
        ChatMessage m = new ChatMessage(); m.setId(3L); m.setText("My VPN is down");
        m.setFromUser(true); m.setCreatedAt(Instant.parse("2026-11-21T00:00:00Z"));
        m.setMessageFrustrationScore(90);
        when(messages.findTop201BySessionOrderByIdDesc(session)).thenReturn(List.of(m));
        var result = chat.history("emp");
        assertEquals(1, result.size());
        assertFalse(result.getFirst().containsKey("messageFrustrationScore"));
        assertFalse(result.getFirst().containsKey("frustrationLevel"));
    }
}
