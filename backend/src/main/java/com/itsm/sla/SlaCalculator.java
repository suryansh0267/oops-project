package com.itsm.sla;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Pure SLA arithmetic, kept free of Spring/JPA so it is trivially unit-testable.
 * Used for SLA risk detection (intelligent feature E) and for the SLA filter.
 */
public final class SlaCalculator {

    /** Fraction of the resolution window after which an open incident is "at risk". */
    public static final double AT_RISK_THRESHOLD = 0.75;

    private SlaCalculator() {
    }

    public static SlaStatus status(LocalDateTime createdAt,
                                   LocalDateTime responseDeadline,
                                   LocalDateTime resolutionDeadline,
                                   LocalDateTime respondedAt,
                                   LocalDateTime resolvedAt,
                                   boolean open,
                                   LocalDateTime now) {
        if (!open) {
            if (resolvedAt == null || resolutionDeadline == null) {
                return SlaStatus.MET;
            }
            return resolvedAt.isAfter(resolutionDeadline) ? SlaStatus.MISSED : SlaStatus.MET;
        }
        if (isResponseBreached(responseDeadline, respondedAt, now)
                || (resolutionDeadline != null && now.isAfter(resolutionDeadline))) {
            return SlaStatus.BREACHED;
        }
        return elapsedFraction(createdAt, resolutionDeadline, now) >= AT_RISK_THRESHOLD
                ? SlaStatus.AT_RISK
                : SlaStatus.ON_TRACK;
    }

    public static boolean isResponseBreached(LocalDateTime responseDeadline,
                                             LocalDateTime respondedAt,
                                             LocalDateTime now) {
        if (responseDeadline == null) {
            return false;
        }
        LocalDateTime reference = respondedAt != null ? respondedAt : now;
        return reference.isAfter(responseDeadline);
    }

    /** 0.0 at creation, 1.0 at the deadline, above 1.0 once breached. */
    public static double elapsedFraction(LocalDateTime start, LocalDateTime deadline, LocalDateTime now) {
        if (start == null || deadline == null) {
            return 0.0;
        }
        long window = Duration.between(start, deadline).toSeconds();
        if (window <= 0) {
            return 1.0;
        }
        long elapsed = Duration.between(start, now).toSeconds();
        return Math.max(0.0, (double) elapsed / window);
    }

    /** Minutes left until the deadline; negative when overdue. */
    public static long minutesRemaining(LocalDateTime deadline, LocalDateTime now) {
        if (deadline == null) {
            return Long.MAX_VALUE;
        }
        return Duration.between(now, deadline).toMinutes();
    }
}
