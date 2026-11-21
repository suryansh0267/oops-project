package com.itsm.intelligence.frustration;

import com.itsm.intelligence.TextSimilarity;
import org.springframework.stereotype.Component;

/** Pattern: Strategy. Incident history and repeated requests add independent evidence. */
@Component
public class BehaviourFrustrationSignal implements FrustrationSignal {
    @Override public SignalResult evaluate(FrustrationInput i) {
        double score = 0;
        if (i.reopenCount() > 0) score += Math.min(.4, i.reopenCount() * .2);
        if (i.chasingCount() > 1) score += .16;
        if (i.slaAtRisk()) score += .20;
        if (i.unacknowledgedUrgent()) score += .18;
        if (i.lowRating() > 0 && i.lowRating() <= 2) score += .28;
        if (i.reassignmentCount() > 1) score += .15;
        if (i.pingPongCount() > 2) score += .16;
        if (i.repeatIncidents() > 1) score += .14;
        if (i.repeatedFallback()) score += .22;
        if (i.previousText() != null && !i.previousText().isBlank() && i.text() != null
                && i.text().length() > 8 && TextSimilarity.jaccard(i.text(), i.previousText()) > .80) score += .25;
        return new SignalResult("behaviour", score, .65, "Repeated follow-up or service history");
    }
}
