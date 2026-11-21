package com.itsm.state;

import com.itsm.enums.IncidentStatus;
import com.itsm.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentStateTests {

    @Test
    @DisplayName("Every status has a state object")
    void everyStatusRegistered() {
        for (IncidentStatus s : IncidentStatus.values()) {
            assertEquals(s, IncidentStateFactory.of(s).status());
        }
    }

    @Test
    @DisplayName("NEW -> CLOSED is rejected (must go through the lifecycle)")
    void cannotSkipToClosed() {
        assertThrows(InvalidStateTransitionException.class,
                () -> IncidentStateFactory.assertTransition(IncidentStatus.NEW, IncidentStatus.CLOSED));
    }

    @Test
    @DisplayName("Happy path NEW..CLOSED is allowed step by step")
    void happyPath() {
        IncidentStatus[] path = {IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED,
                IncidentStatus.VERIFIED, IncidentStatus.CLOSED};
        for (int i = 0; i < path.length - 1; i++) {
            final int k = i;
            assertDoesNotThrow(() -> IncidentStateFactory.assertTransition(path[k], path[k + 1]));
        }
    }

    @Test
    @DisplayName("CLOSED is terminal and not open")
    void closedIsTerminal() {
        IncidentState closed = IncidentStateFactory.of(IncidentStatus.CLOSED);
        assertTrue(closed.allowedTransitions().isEmpty());
        assertFalse(closed.isOpen());
    }

    @Test
    @DisplayName("RESOLVED can be reopened to IN_PROGRESS; in-flight work can be reassigned")
    void reopenAndReassign() {
        assertTrue(IncidentStateFactory.of(IncidentStatus.RESOLVED).canTransitionTo(IncidentStatus.IN_PROGRESS));
        assertTrue(IncidentStateFactory.of(IncidentStatus.IN_PROGRESS).canTransitionTo(IncidentStatus.ASSIGNED));
        assertFalse(IncidentStateFactory.of(IncidentStatus.CLASSIFIED).canTransitionTo(IncidentStatus.RESOLVED));
    }

    @Test
    @DisplayName("Open statuses are exactly the ones with a running SLA clock")
    void openStatuses() {
        assertEquals(5, IncidentStateFactory.openStatuses().size());
        assertFalse(IncidentStateFactory.openStatuses().contains(IncidentStatus.RESOLVED));
    }
}
