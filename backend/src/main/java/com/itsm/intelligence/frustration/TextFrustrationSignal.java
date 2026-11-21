package com.itsm.intelligence.frustration;

import org.springframework.stereotype.Component;
import java.util.Locale;

/** Service-directed text cues; ordinary technical failures are deliberately neutral. */
@Component
public class TextFrustrationSignal implements FrustrationSignal {
    @Override public SignalResult evaluate(FrustrationInput input) {
        String raw = input.text() == null ? "" : input.text();
        String t = raw.toLowerCase(Locale.ROOT);
        double score = 0;
        String reason = "";
        if (t.matches(".*(still not (fixed|working)|any update|no (one|body) (replies|responds)|waiting (for|since)).*")) {
            score += .43; reason = "Repeated wait or unresolved issue";
        }
        if (t.matches(".*(third time|3rd time|again and again|every (single )?time|unacceptable|useless|terrible support).*")) {
            score += .72; reason = "Repeated failures or poor support";
        }
        if (t.matches(".*(manager|escalate|speak to (a |some)?(human|person|someone)|supervisor).*")) {
            score = 1.0; reason = "Explicit request for escalation";
        }
        if (t.contains("!!") || t.contains("??")) score += .16;
        long letters = raw.chars().filter(Character::isLetter).count();
        long upper = raw.chars().filter(Character::isUpperCase).count();
        if (letters >= 7 && upper * 1.0 / letters > .75) score += .35;
        if (t.contains("[redacted]")) score += .30;
        if (score == 0 && t.matches(".*(crashed|failed|not responding|is down|error|timeout).*"))
            return new SignalResult("text", 0, 1, "Technical issue, not a complaint about support");
        return new SignalResult("text", score, 1, reason.isBlank() ? "Angry emphasis" : reason);
    }
}
