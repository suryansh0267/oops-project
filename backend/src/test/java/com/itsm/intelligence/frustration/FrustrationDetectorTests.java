package com.itsm.intelligence.frustration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Calibration fixtures are pure JUnit tests without a database or Spring context. */
class FrustrationDetectorTests {
    private final Instant now = Instant.parse("2026-11-21T08:00:00Z");
    private FrustrationDetector detector(Instant instant) {
        return new FrustrationDetector(List.of(new TextFrustrationSignal(), new BehaviourFrustrationSignal()),
                Clock.fixed(instant, ZoneOffset.UTC));
    }
    private FrustrationAssessment assess(String text, int previous, Instant previousAt) {
        return detector(now).assess(FrustrationInput.text(text, null), previous, previousAt);
    }

    @ParameterizedTest
    @CsvSource({
            "the database server is not responding,CALM",
            "app crashed unexpectedly,CALM",
            "login failed with a timeout,CALM",
            "still not working any update?,ANNOYED",
            "still not fixed!!,ANNOYED"
    })
    void technicalFailuresDoNotBecomePersonalFrustration(String text, FrustrationLevel expected) {
        assertEquals(expected, assess(text, 0, null).level());
    }

    @Test void escalatingDemoLadderReachesCriticalInThreeTurns() {
        FrustrationAssessment one = assess("still not fixed!!", 0, null);
        FrustrationAssessment two = assess("THIRD TIME this week, useless", one.score(), now);
        FrustrationAssessment three = assess("I demand a manager, escalate this", two.score(), now);
        assertEquals(FrustrationLevel.ANNOYED, one.level());
        assertEquals(FrustrationLevel.FRUSTRATED, two.level());
        assertEquals(FrustrationLevel.CRITICAL, three.level());
        assertEquals("RISING", three.trend());
    }

    @Test void demandAloneDoesNotInstantlyBecomeCritical() {
        FrustrationAssessment a = assess("I want to speak to a manager", 0, null);
        assertEquals(FrustrationLevel.FRUSTRATED, a.level());
    }

    @Test void singleAllCapsOutburstCannotBeCritical() {
        assertNotEquals(FrustrationLevel.CRITICAL, assess("THIS IS REALLY UNACCEPTABLE!!", 0, null).level());
    }

    @Test void previousFrustrationDecaysAfterCalmTime() {
        FrustrationAssessment calmer = detector(now.plusSeconds(60 * 60 * 72)).assess(
                FrustrationInput.text("thank you, working fine", null), 85, now);
        assertEquals(FrustrationLevel.CALM, calmer.level());
        assertEquals("FALLING", calmer.trend());
    }

    @Test void repeatedMessagesAndFallbackAddBehaviouralSignal() {
        FrustrationInput input = new FrustrationInput("any update on this?", "any update on this?", true,
                1, 3, true, true, 1, 2, 4, 3);
        assertTrue(new BehaviourFrustrationSignal().evaluate(input).score() > .5);
    }

    @Test void serviceNegationIsDifferentFromPersonalComplaint() {
        assertEquals(0, new TextFrustrationSignal().evaluate(
                FrustrationInput.text("database server is not responding", null)).score());
    }

    @ParameterizedTest
    @CsvSource({"damn,[redacted]", "what the fuck,what the [redacted]", "SHIT! this is awful,[redacted]! this is awful"})
    void profanityIsMaskedBeforeStorage(String raw, String expected) {
        assertEquals(expected, ProfanitySanitizer.mask(raw));
        assertFalse(ProfanitySanitizer.contains(ProfanitySanitizer.mask(raw)));
    }

    @Test void reasonsAreImmutableAndBounded() {
        FrustrationAssessment a = assess("THIRD TIME this week, useless!!", 40, now);
        assertTrue(a.reasons().size() <= 3);
        assertThrows(UnsupportedOperationException.class, () -> a.reasons().add("fake"));
    }

    @Test void debounceSuppressesSameLevelButAllowsCriticalRiseInsideCooldown() {
        assertTrue(FrustrationAlertPolicy.shouldNotify(FrustrationLevel.ANNOYED,
                FrustrationLevel.FRUSTRATED, null, now, 30));
        assertFalse(FrustrationAlertPolicy.shouldNotify(FrustrationLevel.FRUSTRATED,
                FrustrationLevel.FRUSTRATED, now, now.plusSeconds(30), 30));
        assertTrue(FrustrationAlertPolicy.shouldNotify(FrustrationLevel.FRUSTRATED,
                FrustrationLevel.CRITICAL, now, now.plusSeconds(30), 30));
    }
}
