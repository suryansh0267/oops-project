package com.itsm.intelligence.frustration;

/** Text is already sanitized; behaviour is incident-related context supplied by the caller. */
public record FrustrationInput(String text, String previousText, boolean repeatedFallback,
                               int reopenCount, int chasingCount, boolean slaAtRisk,
                               boolean unacknowledgedUrgent, int lowRating,
                               int reassignmentCount, int pingPongCount, int repeatIncidents) {
    public static FrustrationInput text(String value, String previous) {
        return new FrustrationInput(value, previous, false, 0, 0, false, false, 0, 0, 0, 0);
    }
}
