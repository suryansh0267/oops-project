package com.itsm.intelligence.frustration;

import com.itsm.entity.*;
import com.itsm.enums.UserRole;
import com.itsm.repository.*;
import com.itsm.security.IncidentAccessPolicy;
import com.itsm.service.IncidentMapper;
import com.itsm.service.AuditService;
import com.itsm.state.IncidentStateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Per-user, per-incident frustration with bounded scheduled behavioural re-checks. */
@Slf4j @Service @RequiredArgsConstructor
public class FrustrationService {
    private final FrustrationStateRepository states;
    private final FrustrationRecordRepository records;
    private final FrustrationDetector detector;
    private final IncidentRepository incidents;
    private final IncidentNoteRepository notes;
    private final AuditLogRepository audits;
    private final IncidentAccessPolicy access;
    private final IncidentMapper mapper;
    private final AuditService audit;
    private final PlatformTransactionManager txManager;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    @Value("${app.frustration.enabled:true}") private boolean enabled = true;
    @Value("${app.frustration.cooldown-minutes:30}") private long cooldownMinutes = 30;
    @Value("${app.frustration.repeat-window-days:7}") private long repeatWindowDays = 7;

    @Transactional
    public FrustrationAssessment observe(User user, Incident incident, String sanitizedText, boolean fallback) {
        if (!enabled || user == null) return new FrustrationAssessment(0, FrustrationLevel.CALM, List.of(), "STEADY");
        sanitizedText = ProfanitySanitizer.mask(sanitizedText);
        if (incident != null) access.assertCanView(user, incident);
        String key = user.getId() + ":" + (incident == null ? "none" : incident.getId());
        FrustrationState state = states.findById(key).orElseGet(() -> {
            FrustrationState s = new FrustrationState(); s.setSubjectKey(key); s.setUser(user); s.setIncident(incident);
            return s;
        });
        FrustrationInput input = input(user, incident, sanitizedText, state, fallback);
        FrustrationAssessment result = detector.assess(input, state.getScore(), state.getAssessedAt());
        FrustrationLevel old = state.getLevel() == null ? FrustrationLevel.CALM : state.getLevel();
        state.setScore(result.score()); state.setLevel(result.level()); state.setAssessedAt(clock.instant());
        if (sanitizedText != null && !sanitizedText.isBlank())
            state.setLastMessage(sanitizedText.substring(0, Math.min(1000, sanitizedText.length())));
        state.setFallbackStreak(fallback ? state.getFallbackStreak() + 1 : 0);
        boolean rising = result.level().ordinal() > old.ordinal();
        if (rising) state.setDismissed(false);
        boolean alert = incident != null && user.getRole() == UserRole.EMPLOYEE
                && FrustrationAlertPolicy.shouldNotify(old, result.level(), state.getNotifiedAt(),
                        clock.instant(), cooldownMinutes);
        if (alert) state.setNotifiedAt(clock.instant());
        states.save(state);
        if (alert) {
            try { events.publishEvent(new FrustrationDetectedEvent(user, incident, result)); }
            catch (RuntimeException ex) { log.warn("Frustration alert delivery failed for incident {}", incident.getId()); }
        }
        return result;
    }

    private FrustrationInput input(User user, Incident incident, String text, FrustrationState state, boolean fallback) {
        if (incident == null) return new FrustrationInput(text, state.getLastMessage(), fallback && state.getFallbackStreak() > 0,
                0, 0, false, false, 0, 0, 0, 0);
        List<AuditLog> history = audits.findByIncidentOrderByTimestampAsc(incident);
        long reopened = history.stream().filter(a -> "REOPENED".equals(a.getAction())).count();
        long reassigned = history.stream().filter(a -> a.getAction() != null && a.getAction().contains("ASSIGN")).count();
        List<IncidentNote> thread = notes.findByIncidentOrderByCreatedAtAsc(incident);
        long chasing = thread.stream().filter(n -> n.getAuthor() != null && Objects.equals(n.getAuthor().getId(), user.getId())
                && n.getContent() != null && n.getContent().toLowerCase(Locale.ROOT).contains("update")).count();
        long pingPong = thread.stream().filter(n -> n.getContent() != null && n.getContent().startsWith("[INFO_")).count();
        boolean urgent = incident.getPriority() != null && incident.getPriority().name().matches("P[12]_.*")
                && incident.getRespondedAt() == null;
        boolean risk = mapper.slaStatus(incident, LocalDateTime.now()).name().matches("AT_RISK|BREACHED");
        long repeats = incidents.findByReporterOrderByCreatedAtDesc(user).stream()
                .filter(i -> Objects.equals(i.getAffectedService(), incident.getAffectedService()) && i.getAffectedService() != null)
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(LocalDateTime.now().minusDays(repeatWindowDays)))
                .count();
        return new FrustrationInput(text, state.getLastMessage(), fallback && state.getFallbackStreak() > 0,
                (int) reopened, (int) chasing, risk, urgent, incident.getFeedbackRating(),
                (int) reassigned, (int) pingPong, (int) repeats);
    }

    @Transactional
    public void dismiss(Long id, User staff) {
        if (staff.getRole() == UserRole.EMPLOYEE) throw new AccessDeniedException("Staff only");
        Incident i = incidents.findById(id).orElseThrow(() -> new com.itsm.exception.ResourceNotFoundException("Incident", "id", id));
        access.assertCanView(staff, i);
        i.setFrustrationLevel(null); i.setFrustrationReasons(null);
        incidents.save(i);
        states.findByIncidentId(id).forEach(s -> { s.setDismissed(true); states.save(s); });
        audit.logAction(i, staff, "FRUSTRATION_DISMISSED", null, null, "Staff dismissed heuristic frustration flag");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<Incident> flagged = incidents.findAll().stream().filter(i -> IncidentStateFactory.of(i.getStatus()).isOpen())
                .filter(i -> i.getFrustrationLevel() != null
                && i.getFrustrationLevel().ordinal() >= FrustrationLevel.FRUSTRATED.ordinal()).toList();
        Map<String, Long> services = flagged.stream().collect(Collectors.groupingBy(i ->
                i.getAffectedService() == null ? "Unspecified" : i.getAffectedService().getName(), Collectors.counting()));
        LocalDate today = LocalDate.now(clock);
        List<FrustrationRecord> all = records.findByRecordedAtAfterOrderByRecordedAtAsc(today.minusDays(13).atStartOfDay(clock.getZone()).toInstant());
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int d=13; d>=0; d--) {
            LocalDate day = today.minusDays(d);
            long count = all.stream().filter(r -> r.getRecordedAt().atZone(clock.getZone()).toLocalDate().equals(day)).count();
            trend.add(Map.of("date", day.toString(), "count", count));
        }
        return Map.of("count", flagged.size(), "incidents", flagged.stream().map(i -> Map.of(
                "id", i.getId(), "incidentNumber", i.getIncidentNumber(), "level", i.getFrustrationLevel().name(),
                "reasons", i.getFrustrationReasons() == null ? "" : i.getFrustrationReasons())).toList(),
                "services", services, "trend", trend);
    }

    @Scheduled(fixedDelayString = "${app.frustration.recheck-interval-ms:60000}", initialDelay = 45000)
    public void recheck() {
        if (!enabled) return;
        List<Long> ids = incidents.findAll(PageRequest.of(0, 100)).getContent().stream().map(Incident::getId).toList();
        TransactionTemplate tx = new TransactionTemplate(txManager);
        for (Long id : ids) {
            try {
                tx.executeWithoutResult(status -> incidents.findById(id).ifPresent(i -> {
                    if (IncidentStateFactory.of(i.getStatus()).isOpen() && i.getReporter() != null)
                        observe(i.getReporter(), i, "", false);
                }));
            } catch (RuntimeException ex) { log.warn("Frustration re-check failed for incident {}", id); }
        }
    }
}
