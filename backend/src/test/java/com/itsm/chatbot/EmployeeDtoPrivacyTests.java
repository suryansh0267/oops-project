package com.itsm.chatbot;

import com.itsm.dto.IncidentResponse;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeDtoPrivacyTests {
    @Test void incidentDtoNeverExposesFrustrationScoreOrReasons() {
        var names = Arrays.stream(IncidentResponse.class.getDeclaredFields()).map(f -> f.getName()).toList();
        assertFalse(names.contains("frustrationLevel"));
        assertFalse(names.contains("frustrationScore"));
        assertFalse(names.contains("frustrationReasons"));
    }
}
