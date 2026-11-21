package com.itsm.sla;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SlaCalculatorTests {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 11, 20, 10, 0);

    @Test
    @DisplayName("Open incident early in its window is ON_TRACK")
    void onTrack() {
        assertEquals(SlaStatus.ON_TRACK, SlaCalculator.status(t0, t0.plusMinutes(15), t0.plusMinutes(120),
                t0.plusMinutes(5), null, true, t0.plusMinutes(30)));
    }

    @Test
    @DisplayName("Past 75% of the resolution window is AT_RISK")
    void atRisk() {
        assertEquals(SlaStatus.AT_RISK, SlaCalculator.status(t0, t0.plusMinutes(15), t0.plusMinutes(120),
                t0.plusMinutes(5), null, true, t0.plusMinutes(95)));
    }

    @Test
    @DisplayName("Not acknowledged by the response deadline is BREACHED")
    void responseBreach() {
        assertEquals(SlaStatus.BREACHED, SlaCalculator.status(t0, t0.plusMinutes(5), t0.plusMinutes(30),
                null, null, true, t0.plusMinutes(6)));
    }

    @Test
    @DisplayName("Boundary: exactly at the response deadline is not yet breached")
    void exactlyAtDeadline() {
        assertFalse(SlaCalculator.isResponseBreached(t0.plusMinutes(5), null, t0.plusMinutes(5)));
    }

    @Test
    @DisplayName("Resolved before / after the deadline is MET / MISSED")
    void metAndMissed() {
        assertEquals(SlaStatus.MET, SlaCalculator.status(t0, t0.plusMinutes(5), t0.plusMinutes(30),
                t0.plusMinutes(2), t0.plusMinutes(25), false, t0.plusHours(5)));
        assertEquals(SlaStatus.MISSED, SlaCalculator.status(t0, t0.plusMinutes(5), t0.plusMinutes(30),
                t0.plusMinutes(2), t0.plusMinutes(31), false, t0.plusHours(5)));
    }

    @Test
    @DisplayName("Elapsed fraction handles missing data and zero windows")
    void elapsedEdgeCases() {
        assertEquals(0.0, SlaCalculator.elapsedFraction(null, t0, t0));
        assertEquals(1.0, SlaCalculator.elapsedFraction(t0, t0, t0));
        assertEquals(0.5, SlaCalculator.elapsedFraction(t0, t0.plusMinutes(60), t0.plusMinutes(30)), 0.001);
    }
}
