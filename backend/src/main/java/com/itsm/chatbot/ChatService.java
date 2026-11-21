package com.itsm.chatbot;

import com.itsm.dto.*;
import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.intelligence.frustration.*;
import com.itsm.repository.*;
import com.itsm.security.IncidentAccessPolicy;
import com.itsm.service.*;
import com.itsm.event.*;
import com.itsm.state.IncidentStateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * Coordinates the persisted report dialogue (Pattern: State), rule/LLM strategy,
 * incident access policy, frustration observer, and existing incident service.
 */
@Slf4j @Service @RequiredArgsConstructor
public class ChatService {
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final IncidentRepository incidents;
    private final ITServiceRepository services;
    private final IncidentService incidentService;
    private final IncidentCategoryService categories;
    private final DuplicateDetectionService duplicates;
    private final DashboardService dashboard;
    private final IncidentAccessPolicy access;
    private final FrustrationService frustration;
    private final ChatEngineRegistry engines;
    private final IntentClassifier classifier;
    private final ChatRateLimiter limiter;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public ChatReply message(String username, String incoming) {
        limiter.check(username);
        User user = user(username);
        ChatSession session = getOrCreate(user, true);
        String safe = ProfanitySanitizer.mask(incoming).strip();
        IntentClassifier.Result classified = classifier.classify(safe, user.getRole());
        Incident selected;
        boolean inaccessible = false;
        try { selected = selectedIncident(user, session, classified.incidentNumber()); }
        catch (org.springframework.security.access.AccessDeniedException | ResourceNotFoundException ex) {
            selected = null; inaccessible = true;
        }
        FrustrationAssessment sentiment = frustration.observe(user, selected, safe,
                classified.intent() == Intent.FALLBACK);
        save(session, true, safe, classified.intent().name(), sentiment.score(), selected);
        ChatReply reply;
        try {
            reply = inaccessible
                    ? ChatReply.say("I can't access that incident. I can help with your own reports or create a new one.")
                    : respond(user, session, safe, classified, selected);
            if (sentiment.level().ordinal() >= FrustrationLevel.ANNOYED.ordinal()) {
                reply = new ChatReply("I'm sorry this has been frustrating. " + reply.text(),
                        reply.cards(), reply.quickReplies(), reply.actions());
            }
            if (sentiment.level().ordinal() >= FrustrationLevel.FRUSTRATED.ordinal()
                    && session.getState() != ChatState.AWAIT_HANDOFF) {
                List<String> quick = new ArrayList<>(reply.quickReplies());
                quick.add("Speak to a person");
                reply = new ChatReply(reply.text() + " I can request a human follow-up if you'd like.",
                        reply.cards(), quick, reply.actions());
            }
            // Optional LLM can rephrase the template, never choose or execute an action.
            String phrased = ProfanitySanitizer.mask(engines.engine().phrase(reply.text(), safe));
            if (phrased != null && phrased.length() <= 1800 && !phrased.isBlank())
                reply = new ChatReply(phrased, reply.cards(), reply.quickReplies(), reply.actions());
        } catch (org.springframework.security.access.AccessDeniedException | ResourceNotFoundException denied) {
            reply = ChatReply.say("I can't access that incident. I can help with your own reports or create a new one.");
        }
        session.setUpdatedAt(clock.instant()); sessions.save(session);
        save(session, false, reply.text(), classified.intent().name(), null, selected);
        prune(session);
        return reply;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(String username) {
        User user = user(username);
        return sessions.findByUser(user).map(s -> {
            List<ChatMessage> last = new ArrayList<>(messages.findTop201BySessionOrderByIdDesc(s));
            Collections.reverse(last);
            return last.stream().map(m -> Map.<String, Object>of("id", m.getId(), "fromUser", m.isFromUser(),
                    "text", m.getText(), "createdAt", m.getCreatedAt().toString())).toList();
        }).orElse(List.of());
    }

    @Transactional
    public ChatReply reset(String username) {
        ChatSession session = getOrCreate(user(username), true);
        session.clearDraft(); sessions.save(session);
        return ChatReply.say("I've cancelled the draft. How can I help?");
    }

    private ChatSession getOrCreate(User user, boolean locked) {
        return (locked ? sessions.findLockedByUser(user) : sessions.findByUser(user))
                .orElseGet(() -> {
                    ChatSession session = new ChatSession(); session.setUser(user);
                    session.setUpdatedAt(clock.instant()); return sessions.saveAndFlush(session);
                });
    }

    private ChatReply respond(User user, ChatSession s, String safe,
                              IntentClassifier.Result intent, Incident incident) {
        String t = safe.toLowerCase(Locale.ROOT);
        if (t.matches(".*(cancel|restart|start over|abort|nevermind|clear draft).*")) {
            s.clearDraft(); return ChatReply.say("Draft cancelled. Tell me what you need help with.");
        }
        if (intent.intent() == Intent.REQUEST_HUMAN) {
            s.setState(ChatState.AWAIT_HANDOFF);
            return new ChatReply("I can request a human follow-up, but this is not a live chat. Confirm to add a support request to your incident.",
                    List.of("Confirm handoff", "Cancel"));
        }
        // The state takes precedence over ordinary intent keywords, but cancel/handoff work anytime.
        if (s.getState() != ChatState.IDLE) return advance(user, s, safe, incident);
        return switch (intent.intent()) {
            case REPORT_INCIDENT -> startReport(s, safe);
            case CHECK_STATUS -> status(user, incident, t);
            case TROUBLESHOOT -> troubleshoot(t);
            case SLA_QUESTION -> sla(user, incident);
            case ADD_UPDATE -> startUpdate(s, incident, safe);
            case MY_QUEUE -> user.getRole() == UserRole.EMPLOYEE
                    ? ChatReply.say("That action is for technicians. I can check your incident status instead.") : queue(user);
            case SUMMARIZE_INCIDENT -> user.getRole() == UserRole.EMPLOYEE
                    ? ChatReply.say("Technician summaries are for staff. I can check your own incident status.")
                    : summary(user, incident);
            case SYSTEM_SUMMARY -> user.getRole() != UserRole.ADMIN
                    ? ChatReply.say("System-wide statistics are available to administrators only.") : systemSummary();
            case GREETING, HELP -> new ChatReply("I can report incidents, show your status, suggest safe fixes and request a human follow-up."
                    + (user.getRole() == UserRole.TECHNICIAN ? " I can also show your queue." : "")
                    + (user.getRole() == UserRole.ADMIN ? " I can also show system statistics." : ""),
                    List.of("Report an incident", "My open incidents", "How do I fix VPN?"));
            default -> new ChatReply("I'm not sure yet. Try 'report an incident', 'my open incidents', or 'help'.",
                    List.of("Report an incident", "My open incidents", "How do I fix VPN?"));
        };
    }

    private ChatReply startReport(ChatSession s, String description) {
        s.clearDraft();
        if (!description.toLowerCase(Locale.ROOT).matches(".*(report|raise (a )?ticket|incident).*")) {
            s.setDraftDescription(description);
        }
        s.setState(ChatState.AWAIT_SERVICE);
        List<String> names = services.findAll().stream().filter(ITService::isActive)
                .map(ITService::getName).limit(8).toList();
        List<String> quick = new ArrayList<>(names); quick.add("Not sure");
        return new ChatReply("Let's report this. Which IT service is affected?", quick);
    }

    private ChatReply advance(User user, ChatSession s, String safe, Incident incident) {
        String t = safe.toLowerCase(Locale.ROOT).strip();
        switch (s.getState()) {
            case AWAIT_SERVICE -> {
                ITService selected = services.findAll().stream().filter(ITService::isActive)
                        .filter(v -> v.getName().equalsIgnoreCase(t) || v.getName().toLowerCase(Locale.ROOT).contains(t)
                                || (t.contains("network") && v.getName().toLowerCase(Locale.ROOT).contains("network"))
                                || (t.contains("vpn") && v.getName().toLowerCase(Locale.ROOT).contains("vpn"))
                                || (t.contains("email") && v.getName().toLowerCase(Locale.ROOT).contains("email"))
                                || (t.contains("mail") && v.getName().toLowerCase(Locale.ROOT).contains("email"))
                                || (t.contains("login") && v.getName().toLowerCase(Locale.ROOT).contains("authentication"))
                                || (t.contains("auth") && v.getName().toLowerCase(Locale.ROOT).contains("authentication"))
                                || (t.contains("db") && v.getName().toLowerCase(Locale.ROOT).contains("database"))
                                || (t.contains("database") && v.getName().toLowerCase(Locale.ROOT).contains("database"))
                                || (t.contains("pay") && v.getName().toLowerCase(Locale.ROOT).contains("payment"))
                                || (t.contains("storage") && v.getName().toLowerCase(Locale.ROOT).contains("storage"))
                                || (t.contains("cloud") && v.getName().toLowerCase(Locale.ROOT).contains("storage")))
                        .findFirst().orElse(null);
                if (selected == null && !t.matches(".*(not sure|none|skip|other|no).*")) {
                    return new ChatReply("I couldn't match that service. Choose one of the listed services or 'Not sure'.",
                            services.findAll().stream().filter(ITService::isActive).map(ITService::getName).limit(8).toList());
                }
                if (selected != null) { s.setDraftServiceId(selected.getId()); s.setDraftServiceName(selected.getName()); }
                s.setState(ChatState.AWAIT_DESCRIPTION);
                return new ChatReply(s.getDraftDescription() == null
                        ? "Describe what went wrong and when it started."
                        : "I noted your initial description: \"" + s.getDraftDescription() + "\". Add details, or choose 'Continue' to use it.",
                        s.getDraftDescription() == null ? List.of() : List.of("Continue"));
            }
            case AWAIT_DESCRIPTION -> {
                if (!t.equals("continue") && !t.equals("skip")) {
                    String combined = s.getDraftDescription() == null ? safe : s.getDraftDescription() + ". " + safe;
                    if (combined.length() > 5000) return ChatReply.say("The report description is too long. Please shorten it or start over.");
                    s.setDraftDescription(combined);
                }
                if (s.getDraftDescription() == null || s.getDraftDescription().length() < 5)
                    return ChatReply.say("Please describe the problem in at least five characters.");
                s.setDraftTitle(s.getDraftDescription().length() > 100
                        ? s.getDraftDescription().substring(0, 100) : s.getDraftDescription());
                String category = categories.suggest(s.getDraftTitle(), s.getDraftDescription()).category();
                s.setDraftCategory(category);
                ChatEngine.DraftExtraction extracted = engines.engine().extract(s.getDraftDescription());
                if (extracted != null) {
                    if (extracted.title() != null && extracted.title().length() >= 5 && extracted.title().length() <= 150) s.setDraftTitle(extracted.title());
                    if (extracted.category() != null) {
                        try { s.setDraftCategory(categories.requireActive(extracted.category())); }
                        catch (RuntimeException ignored) { /* classifier remains authoritative */ }
                    }
                    if (extracted.impact() != null) {
                        try { s.setDraftImpact(Impact.valueOf(extracted.impact().toUpperCase(Locale.ROOT)).name()); }
                        catch (IllegalArgumentException ignored) { }
                    }
                    if (extracted.urgency() != null) {
                        try { s.setDraftUrgency(Urgency.valueOf(extracted.urgency().toUpperCase(Locale.ROOT)).name()); }
                        catch (IllegalArgumentException ignored) { }
                    }
                    if (extracted.affectedUsers() != null && extracted.affectedUsers() >= 1 && extracted.affectedUsers() <= 100000)
                        s.setDraftAffectedUsers(extracted.affectedUsers());
                }
                s.setState(s.getDraftImpact() != null && s.getDraftUrgency() != null
                        ? ChatState.AWAIT_CONFIRM : ChatState.AWAIT_IMPACT);
                if (s.getState() == ChatState.AWAIT_CONFIRM) return preview(s);
                return new ChatReply("How many people are affected? This helps calculate impact and priority.",
                        List.of("Just me", "My team", "Everyone affected", "Not sure"));
            }
            case AWAIT_IMPACT -> {
                if (t.contains("everyone") || t.contains("many") || t.contains("whole") || t.contains("all") || t.contains("company")) {
                    s.setDraftImpact("HIGH"); s.setDraftUrgency("HIGH"); s.setDraftAffectedUsers(100);
                } else if (t.contains("team") || t.contains("department") || t.contains("several") || t.contains("multiple")) {
                    s.setDraftImpact("MEDIUM"); s.setDraftUrgency("MEDIUM"); s.setDraftAffectedUsers(10);
                } else { s.setDraftImpact("LOW"); s.setDraftUrgency("MEDIUM"); s.setDraftAffectedUsers(1); }
                s.setState(ChatState.AWAIT_CONFIRM);
                return preview(s);
            }
            case AWAIT_CONFIRM -> {
                if (confirmed(t)) {
                    IncidentRequest request = new IncidentRequest();
                    request.setTitle(s.getDraftTitle()); request.setDescription(s.getDraftDescription());
                    request.setCategory(s.getDraftCategory()); request.setAffectedServiceId(s.getDraftServiceId());
                    request.setImpact(Impact.valueOf(s.getDraftImpact())); request.setUrgency(Urgency.valueOf(s.getDraftUrgency()));
                    request.setAffectedUsers(s.getDraftAffectedUsers());
                    // Lock on the session + state transition enforces single-use confirmation.
                    IncidentResponse created = incidentService.createIncident(request, user.getUsername());
                    s.clearDraft(); s.setActiveIncidentId(created.getId());
                    return new ChatReply("Created " + created.getIncidentNumber() + " (" + created.getPriority()
                            + "). Current SLA: " + created.getSlaStatus() + "."
                            + (created.getAssignedTechnicianName() != null ? " Assigned to " + created.getAssignedTechnicianName() + "." : " Awaiting assignment."),
                            List.of(new ChatReply.Card(created.getIncidentNumber(), created.getStatusLabel(), created.getTitle())),
                            List.of("Check status"),
                            List.of(new ChatReply.Action("OPEN_INCIDENT", created.getId(), "View incident")));
                }
                return new ChatReply("I haven't created anything. Reply 'Confirm' to submit or 'Cancel' to discard.",
                        List.of("Confirm", "Cancel"));
            }
            case AWAIT_HANDOFF -> {
                if (!confirmed(t)) return new ChatReply("No agent request was sent. Reply 'Confirm handoff' or 'Cancel'.",
                        List.of("Confirm handoff", "Cancel"));
                s.setState(ChatState.IDLE);
                if (incident == null) return ChatReply.say("I don't see an incident to attach this request to. Please report one first.");
                NoteRequest note = new NoteRequest(); note.setContent("[SUPPORT REQUEST] Reporter requested human assistance through the chatbot.");
                incidentService.addNote(incident.getId(), note, user.getUsername());
                events.publishEvent(new IncidentEvent(IncidentEventType.INFO_PROVIDED, incident, user,
                        "Reporter requested human support through the chatbot"));
                return new ChatReply("Human follow-up requested for " + incident.getIncidentNumber() + ". The assigned technician has been notified; this is not a live connection. Current SLA: "
                        + incidentService.getIncidentById(incident.getId(), user.getUsername()).getSlaStatus() + ".",
                        List.of(new ChatReply.Card(incident.getIncidentNumber(), "Support requested", "Follow your incident updates")),
                        List.of(), List.of(new ChatReply.Action("OPEN_INCIDENT", incident.getId(), "Open incident")));
            }
            case AWAIT_NOTE_CONFIRM -> {
                if (!confirmed(t)) return new ChatReply("Reply 'Confirm update' to post the note or 'Cancel'.",
                        List.of("Confirm update", "Cancel"));
                if (incident == null || s.getPendingNote() == null) { s.clearDraft(); return ChatReply.say("No incident is selected. Please identify your incident first."); }
                NoteRequest note = new NoteRequest(); note.setContent(s.getPendingNote());
                incidentService.addNote(incident.getId(), note, user.getUsername());
                s.clearDraft();
                return ChatReply.say("Your update was added to " + incident.getIncidentNumber() + ".");
            }
            default -> { return ChatReply.say("Tell me what you'd like to do next."); }
        }
    }

    private ChatReply preview(ChatSession s) {
        List<SimilarIncident> similar = duplicates.findSimilarToDraft(s.getDraftTitle(), s.getDraftDescription(),
                s.getDraftServiceId(), s.getDraftCategory());
        // A short report ('my vpn is down') may not meet the lexical similarity
        // threshold even though current VPN outages exist. Count safe matches.
        String description = s.getDraftDescription().toLowerCase(Locale.ROOT);
        long contextualMatches = incidents.findByStatusIn(IncidentStateFactory.openStatuses()).stream()
                .filter(i -> (description.contains("vpn") && (i.getTitle() + " " + i.getDescription())
                        .toLowerCase(Locale.ROOT).contains("vpn"))
                        || (s.getDraftServiceId() != null && i.getAffectedService() != null
                        && Objects.equals(i.getAffectedService().getId(), s.getDraftServiceId())))
                .count();
        long matchCount = Math.max(similar.size(), contextualMatches);
        // Never reveal other reporters' incident identifiers, private titles or notes.
        List<ChatReply.Card> cards = new ArrayList<>();
        cards.add(new ChatReply.Card("Draft: " + s.getDraftTitle(), "Category: " + s.getDraftCategory()
                + "; impact: " + s.getDraftImpact() + "; urgency: " + s.getDraftUrgency(),
                "Service: " + (s.getDraftServiceName() == null ? "Unspecified" : s.getDraftServiceName())));
        if (matchCount > 0) cards.add(new ChatReply.Card("Similar reports found", matchCount + " possible matches",
                "Some issues may already be under investigation; other reporters' details remain private."));
        cards.add(new ChatReply.Card("Suggested self-check", "Try this before filing",
                selfFix(s.getDraftCategory())));
        return new ChatReply("Review the draft and suggested fix. If that solves it, cancel. Otherwise confirm to create the incident.",
                cards, List.of("Confirm", "Cancel"),
                List.of(new ChatReply.Action("CONFIRM_CREATE", null, "Confirm"),
                        new ChatReply.Action("CANCEL", null, "Cancel")));
    }

    private ChatReply status(User user, Incident incident, String t) {
        if ((t.contains("my") && !t.contains("inc")) || incident == null) {
            List<IncidentResponse> mine = incidentService.getIncidentsByReporter(user.getUsername()).stream()
                    .filter(i -> !Set.of("CLOSED", "VERIFIED").contains(i.getStatus()))
                    .limit(5).toList();
            if (mine.isEmpty()) return ChatReply.say("You don't have any open incidents. I can help you report one.");
            return new ChatReply("Here are your recent open incidents:", mine.stream().map(i ->
                    new ChatReply.Card(i.getIncidentNumber(), i.getStatusLabel() + " · " + i.getPriority(),
                            i.getTitle() + " · SLA " + i.getSlaStatus())).toList(),
                    List.of("Report an incident"), List.of());
        }
        IncidentResponse i = incidentService.getIncidentById(incident.getId(), user.getUsername());
        return new ChatReply(i.getIncidentNumber() + " is " + i.getStatusLabel() + ". SLA: " + i.getSlaStatus() + ".",
                List.of(new ChatReply.Card(i.getIncidentNumber(), i.getTitle(),
                        "Priority " + i.getPriority() + "; assigned to " +
                                (i.getAssignedTechnicianName() == null ? "the service desk" : i.getAssignedTechnicianName()))),
                List.of("Ask for human help"),
                List.of(new ChatReply.Action("OPEN_INCIDENT", i.getId(), "View incident")));
    }

    private ChatReply troubleshoot(String t) {
        String category = t.contains("vpn") || t.contains("network") || t.contains("wifi") ? "NETWORK"
                : t.contains("password") || t.contains("login") ? "AUTHENTICATION" : "OTHER";
        return new ChatReply("A safe first check: " + selfFix(category) + " If this doesn't work, I can file an incident.",
                List.of("Report an incident"));
    }

    private static String selfFix(String category) {
        if ("NETWORK".equals(category) || "AUTHENTICATION".equals(category))
            return "Check your internet connection, restart the VPN client and try again; never share passwords.";
        if ("SOFTWARE".equals(category) || "APPLICATION".equals(category))
            return "Restart the affected app and check any published service notice; preserve error messages.";
        if ("HARDWARE".equals(category))
            return "Check connections and power. If a battery is swollen or equipment smells hot, stop using it and call IT.";
        return "Note the error and time, check if colleagues are affected, and try a safe restart only if appropriate.";
    }

    private ChatReply sla(User user, Incident incident) {
        if (incident == null) return ChatReply.say("Which of your incidents? Say 'status of INC00001', or ask for your open incidents.");
        IncidentResponse i = incidentService.getIncidentById(incident.getId(), user.getUsername());
        return ChatReply.say("For " + i.getIncidentNumber() + ", SLA is " + i.getSlaStatus()
                + "; response deadline " + i.getResponseDeadline() + ", resolution deadline " + i.getResolutionDeadline() + ".");
    }

    private ChatReply startUpdate(ChatSession s, Incident incident, String safe) {
        if (incident == null) return ChatReply.say("Please specify an incident number before posting an update.");
        String content = safe.contains(":") ? safe.substring(safe.indexOf(':') + 1).trim() : "";
        if (content.length() < 3) return ChatReply.say("Say 'add update to INC00001: your message' to prepare a note.");
        s.setPendingNote(content); s.setState(ChatState.AWAIT_NOTE_CONFIRM);
        return new ChatReply("Post this note to " + incident.getIncidentNumber() + "? " + content,
                List.of("Confirm update", "Cancel"));
    }

    private ChatReply queue(User user) {
        List<IncidentResponse> queue = incidentService.getIncidentsByTechnician(user.getUsername()).stream()
                .filter(i -> !Set.of("CLOSED", "VERIFIED").contains(i.getStatus())).limit(7).toList();
        return new ChatReply(queue.size() + " open incident(s) assigned to you.",
                queue.stream().map(i -> new ChatReply.Card(i.getIncidentNumber(), i.getPriority() + " · " + i.getStatusLabel(), i.getTitle())).toList(),
                List.of(), List.of());
    }

    private ChatReply summary(User user, Incident incident) {
        if (incident == null) return ChatReply.say("Tell me which incident number you'd like summarized.");
        IncidentResponse i = incidentService.getIncidentById(incident.getId(), user.getUsername());
        List<ResolutionSuggestion> suggestions = incidentService.getSuggestedResolutions(i.getId(), user.getUsername());
        return new ChatReply(i.getIncidentNumber() + ": " + i.getTitle() + ". " + i.getDescription()
                + " Status: " + i.getStatusLabel() + "; priority: " + i.getPriority() + "; SLA: " + i.getSlaStatus() + ".",
                suggestions.stream().limit(2).map(r -> new ChatReply.Card("Suggested resolution", "From a resolved report", r.resolutionNotes())).toList(),
                List.of(), List.of(new ChatReply.Action("OPEN_INCIDENT", i.getId(), "View incident")));
    }

    private ChatReply systemSummary() {
        DashboardStats stats = dashboard.getDashboardStats();
        return ChatReply.say("Service desk: " + stats.getTotalIncidents() + " total incidents, " + stats.getOpenIncidents()
                + " open, " + stats.getCriticalIncidents() + " critical, " + stats.getSlaAtRisk() + " at risk, "
                + stats.getSlaViolations() + " SLA breaches.");
    }

    private Incident selectedIncident(User user, ChatSession session, String number) {
        if (number != null) {
            Incident found = incidents.findByIncidentNumberIgnoreCase(number)
                    .orElseThrow(() -> new ResourceNotFoundException("Incident", "number", number));
            // Don't confirm whether another employee's incident exists.
            if (!access.canView(user, found)) throw new org.springframework.security.access.AccessDeniedException("Not accessible");
            session.setActiveIncidentId(found.getId());
            return found;
        }
        if (session.getActiveIncidentId() != null) {
            Optional<Incident> active = incidents.findById(session.getActiveIncidentId());
            if (active.isPresent() && access.canView(user, active.get())) return active.get();
        }
        return incidents.findByReporterOrderByCreatedAtDesc(user).stream()
                .filter(i -> !Set.of(IncidentStatus.CLOSED, IncidentStatus.VERIFIED).contains(i.getStatus()))
                .findFirst().orElse(null);
    }

    private static boolean confirmed(String text) {
        return text.matches(".*(yes|confirm|confirm create|confirm handoff|confirm update|create|submit|go ahead|proceed|sure|ok|okay).*");
    }
    private User user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
    private void save(ChatSession session, boolean fromUser, String text, String intent, Integer score, Incident incident) {
        ChatMessage m = new ChatMessage(); m.setSession(session); m.setFromUser(fromUser);
        m.setText(text); m.setIntent(intent); m.setMessageFrustrationScore(score);
        m.setIncidentId(incident == null ? null : incident.getId()); m.setCreatedAt(clock.instant()); messages.save(m);
    }
    private void prune(ChatSession session) {
        List<ChatMessage> last = messages.findTop201BySessionOrderByIdDesc(session);
        while (last.size() > 200) {
            messages.delete(last.getLast()); messages.flush();
            last = messages.findTop201BySessionOrderByIdDesc(session);
        }
    }
}
