package com.itsm.chatbot;

import com.itsm.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class IntentClassifierTests {
    private final IntentClassifier classifier = new IntentClassifier();

    @ParameterizedTest
    @CsvSource({
            "my vpn is down,REPORT_INCIDENT",
            "please report an incident,REPORT_INCIDENT",
            "status of INC00042,CHECK_STATUS",
            "my open incidents,CHECK_STATUS",
            "how do i fix vpn,TROUBLESHOOT",
            "when will the SLA deadline be,SLA_QUESTION",
            "add update to INC00001: still waiting,ADD_UPDATE",
            "speak to a manager,REQUEST_HUMAN",
            "help,HELP",
            "hello,GREETING",
            "my queue,MY_QUEUE",
            "summarize INC00042,SUMMARIZE_INCIDENT",
            "system summary,SYSTEM_SUMMARY",
            "abracadabra,FALLBACK"
    })
    void intentsMatchPatterns(String input, Intent expected) {
        assertEquals(expected, classifier.classify(input, UserRole.ADMIN).intent());
    }

    @Test void incidentNumberIsCaseInsensitive() {
        assertEquals("INC00042", classifier.classify("Status of inc00042?", UserRole.EMPLOYEE).incidentNumber());
    }

    @Test void nonMatchingIncidentNumberIsIgnored() {
        assertNull(classifier.classify("status INC123456", UserRole.EMPLOYEE).incidentNumber());
    }

    @Test void employeeRequestForStaffIntentIsIdentifiedForRefusal() {
        assertEquals(Intent.SYSTEM_SUMMARY, classifier.classify("system summary", UserRole.EMPLOYEE).intent());
        assertEquals(Intent.MY_QUEUE, classifier.classify("my queue", UserRole.EMPLOYEE).intent());
    }

    @Test void unknownIntentHasLowConfidence() {
        assertTrue(classifier.classify("purple avocado", UserRole.EMPLOYEE).confidence() < .5);
    }
}
