package com.itsm.intelligence.frustration;

public enum FrustrationLevel {
    CALM, ANNOYED, FRUSTRATED, CRITICAL;

    public static FrustrationLevel from(int score, int annoyed, int frustrated, int critical) {
        if (score >= critical) return CRITICAL;
        if (score >= frustrated) return FRUSTRATED;
        if (score >= annoyed) return ANNOYED;
        return CALM;
    }
}
