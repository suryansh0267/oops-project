package com.itsm.intelligence;

/**
 * Smart technician recommendation (intelligent feature D).
 * Scores a technician 0-100 from four explainable factors so the UI can show
 * why someone was recommended.
 */
public final class TechnicianScorer {

    public record Score(double total, double skill, double availability, double experience, double serviceHistory) {
    }

    public static final double SKILL_WEIGHT = 40;
    public static final double AVAILABILITY_WEIGHT = 30;
    public static final double EXPERIENCE_WEIGHT = 15;
    public static final double SERVICE_WEIGHT = 15;

    private TechnicianScorer() {
    }

    /**
     * @param skillMatch          technician lists the incident category as a skill
     * @param active              incidents currently on their plate
     * @param max                 their configured capacity
     * @param totalResolved       lifetime resolved count
     * @param resolvedSameService resolved incidents on the same affected service
     * @param critical            incident is P1/P2 (experience counts double)
     */
    public static Score score(boolean skillMatch, int active, int max, int totalResolved,
                              long resolvedSameService, boolean critical) {
        double skill = skillMatch ? SKILL_WEIGHT : 0;
        double capacity = max <= 0 ? 0 : Math.max(0, 1 - (double) active / max);
        double availability = capacity * AVAILABILITY_WEIGHT;
        double expFactor = Math.min(1.0, totalResolved / 20.0);
        double experience = expFactor * EXPERIENCE_WEIGHT * (critical ? 1.0 : 0.6);
        double serviceHistory = Math.min(1.0, resolvedSameService / 5.0) * SERVICE_WEIGHT;
        double total = round(skill + availability + experience + serviceHistory);
        return new Score(total, round(skill), round(availability), round(experience), round(serviceHistory));
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
