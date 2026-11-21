package com.itsm.intelligence.frustration;

/** Immutable output of a frustration signal (Pattern: Strategy). */
public record SignalResult(String name, double score, double weight, String evidence) {
    public SignalResult {
        score = Math.clamp(score, 0, 1);
        weight = Math.max(0, weight);
    }
}
