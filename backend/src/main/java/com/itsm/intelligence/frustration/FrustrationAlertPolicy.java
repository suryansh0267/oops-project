package com.itsm.intelligence.frustration;

import java.time.Duration;
import java.time.Instant;

/** Side-effect-free alert debounce policy, testable without repositories. */
public final class FrustrationAlertPolicy {
    private FrustrationAlertPolicy() { }
    public static boolean shouldNotify(FrustrationLevel before, FrustrationLevel after,
                                       Instant notifiedAt, Instant now, long cooldownMinutes) {
        if (after.ordinal() <= before.ordinal() || after.ordinal() < FrustrationLevel.FRUSTRATED.ordinal())
            return false;
        return notifiedAt == null || after == FrustrationLevel.CRITICAL
                || Duration.between(notifiedAt, now).toMinutes() >= cooldownMinutes;
    }
}
