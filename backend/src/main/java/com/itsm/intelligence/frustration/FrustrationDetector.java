package com.itsm.intelligence.frustration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Time-decaying evidence aggregation; no Spring context required to unit-test the math. */
@Component
public class FrustrationDetector {
    private final List<FrustrationSignal> signals;
    private final Clock clock;
    @Value("${app.frustration.annoyed:25}") private int annoyed = 25;
    @Value("${app.frustration.frustrated:55}") private int frustrated = 55;
    @Value("${app.frustration.critical:85}") private int critical = 85;
    @Value("${app.frustration.decay-hours:12}") private double decayHours = 12;

    public FrustrationDetector(List<FrustrationSignal> signals, Clock clock) {
        this.signals = signals; this.clock = clock;
    }

    public FrustrationAssessment assess(FrustrationInput input, int previousScore, Instant previousAt) {
        List<SignalResult> results = signals.stream().map(s -> s.evaluate(input)).toList();
        double raw = results.stream().mapToDouble(s -> s.score() * s.weight()).sum();
        raw = Math.min(1, raw);
        double hours = previousAt == null ? 0 : Math.max(0, Duration.between(previousAt, clock.instant()).toMinutes() / 60.0);
        double previous = previousScore * Math.exp(-Math.log(2) * hours / Math.max(1, decayHours));
        boolean demand = input.text() != null && input.text().toLowerCase(java.util.Locale.ROOT)
                .matches(".*(manager|escalate|supervisor|speak to (a |some)?(human|person|someone)).*");
        int score = (int) Math.round(.35 * previous + .65 * raw * 100 + (previous > 25 && raw > .6 ? 7 : 0));
        if (demand) score = Math.max(score, previous >= 55 ? critical : frustrated);
        score = Math.clamp(score, 0, 100);
        List<String> reasons = results.stream().filter(r -> r.score() >= .15)
                .sorted(Comparator.comparingDouble((SignalResult r) -> r.score() * r.weight()).reversed())
                .map(SignalResult::evidence).filter(r -> !r.isBlank()).distinct().limit(3).toList();
        return new FrustrationAssessment(score, FrustrationLevel.from(score, annoyed, frustrated, critical),
                reasons, score > previous + 5 ? "RISING" : score < previous - 5 ? "FALLING" : "STEADY");
    }
}
