package com.itsm.chatbot;

import com.itsm.enums.UserRole;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pattern: Strategy-style deterministic intent classification. Bounded, linear regexes. */
@Component
public class IntentClassifier {
    private static final Pattern NUMBER = Pattern.compile("(?i)\\bINC\\d{5}\\b");
    public record Result(Intent intent, double confidence, String incidentNumber) { }
    public Result classify(String message, UserRole role) {
        String t = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
        Matcher match = NUMBER.matcher(t);
        String number = match.find() ? match.group().toUpperCase(Locale.ROOT) : null;
        Intent intent;
        if (t.matches(".*(manager|escalate|human|agent|person|supervisor|live agent|representative|talk to someone).*")) intent = Intent.REQUEST_HUMAN;
        else if (t.matches(".*(cancel|restart|start over|abort|nevermind).*")) intent = Intent.HELP;
        else if (t.matches(".*(system (summary|overview)|dashboard summary|overall stats).*")) intent = Intent.SYSTEM_SUMMARY;
        else if (t.matches(".*(my (queue|tickets assigned)|assigned to me).*")) intent = Intent.MY_QUEUE;
        else if (t.matches(".*(summari[sz]e|summary of).*")) intent = Intent.SUMMARIZE_INCIDENT;
        else if (t.matches(".*(status|my (open )?incidents|what happened|progress|tracking|my tickets|track ticket).*")) intent = Intent.CHECK_STATUS;
        else if (t.matches(".*(sla|deadline|when will|how long).*")) intent = Intent.SLA_QUESTION;
        else if (t.matches(".*(add (an? )?update|add note|tell the technician|update (my|the) incident).*")) intent = Intent.ADD_UPDATE;
        else if (t.matches(".*(fix|troubleshoot|solution|how (do|can) i|what should i try|steps to).*")) intent = Intent.TROUBLESHOOT;
        else if (t.matches(".*(report|raise|ticket|incident|broken|not working|is down|down\\b|can't|cannot|unable|failed|failing|error|crash|bug|glitch|fault|slow|outage|offline|issue|problem|network|vpn|wifi|internet|connection|connect|login|password|hardware|software|printer|server|database|access).*")) intent = Intent.REPORT_INCIDENT;
        else if (t.matches("^(hi|hello|hey|good (morning|evening|afternoon)|greetings|howdy).*[!. ]*$")) intent = Intent.GREETING;
        else if (t.matches(".*(help|what can you do|options|commands).*")) intent = Intent.HELP;
        else intent = Intent.FALLBACK;
        // Never turn an out-of-role request into an authorised action.
        if (intent == Intent.SYSTEM_SUMMARY && role != UserRole.ADMIN) return new Result(intent, 1, number);
        if ((intent == Intent.MY_QUEUE || intent == Intent.SUMMARIZE_INCIDENT) && role == UserRole.EMPLOYEE)
            return new Result(intent, 1, number);
        return new Result(intent, intent == Intent.FALLBACK ? .2 : .9, number);
    }
}
