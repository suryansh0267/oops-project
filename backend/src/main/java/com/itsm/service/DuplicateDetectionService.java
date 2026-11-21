package com.itsm.service;

import com.itsm.dto.ResolutionSuggestion;
import com.itsm.dto.SimilarIncident;
import com.itsm.entity.Incident;
import com.itsm.enums.IncidentStatus;
import com.itsm.intelligence.TextSimilarity;
import com.itsm.repository.IncidentRepository;
import com.itsm.state.IncidentStateFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Text-similarity based intelligence:
 * duplicate detection (A), incident correlation (B) and suggested resolutions (G).
 */
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    /** Score at or above which two incidents are reported as possible duplicates. */
    public static final double DUPLICATE_THRESHOLD = 0.45;
    /** Lower bar for "related" incidents shown on the detail page. */
    public static final double RELATED_THRESHOLD = 0.30;
    /** Open incidents on the same service within this window are considered correlated. */
    public static final Duration CORRELATION_WINDOW = Duration.ofHours(6);

    private final IncidentRepository incidentRepository;

    /** Open incidents that look like the same problem as {@code incident}. */
    public List<Incident> findPotentialDuplicates(Incident incident) {
        if (incident == null || incident.getTitle() == null) {
            return List.of();
        }
        return openIncidents().stream()
                .filter(other -> !Objects.equals(other.getId(), incident.getId()))
                .filter(other -> score(incident.getTitle(), incident.getDescription(), serviceId(incident),
                        incident.getCategory(), other) >= DUPLICATE_THRESHOLD)
                .sorted(Comparator.comparingDouble((Incident o) -> -score(incident.getTitle(),
                        incident.getDescription(), serviceId(incident), incident.getCategory(), o)))
                .limit(5)
                .toList();
    }

    /** Duplicate check while an employee is still typing a new report. */
    public List<SimilarIncident> findSimilarToDraft(String title, String description, Long serviceId, String category) {
        if ((title == null || title.isBlank()) && (description == null || description.isBlank())) {
            return List.of();
        }
        return openIncidents().stream()
                .map(o -> toSimilar(o, score(title, description, serviceId, category, o), "Similar wording"))
                .filter(s -> s.score() >= DUPLICATE_THRESHOLD)
                .sorted(Comparator.comparingDouble(SimilarIncident::score).reversed())
                .limit(5)
                .toList();
    }

    /**
     * Incident correlation: open incidents that are either textually similar, or hit the
     * same service within a few hours (likely the same underlying outage).
     */
    public List<SimilarIncident> findRelated(Incident incident) {
        Long sid = serviceId(incident);
        LocalDateTime created = incident.getCreatedAt() != null ? incident.getCreatedAt() : LocalDateTime.now();
        return openIncidents().stream()
                .filter(o -> !Objects.equals(o.getId(), incident.getId()))
                .map(o -> {
                    double text = score(incident.getTitle(), incident.getDescription(), sid, incident.getCategory(), o);
                    boolean sameServiceRecent = sid != null && Objects.equals(sid, serviceId(o))
                            && o.getCreatedAt() != null
                            && Duration.between(o.getCreatedAt(), created).abs().compareTo(CORRELATION_WINDOW) <= 0;
                    if (text >= RELATED_THRESHOLD) {
                        return toSimilar(o, text, "Similar wording");
                    }
                    if (sameServiceRecent) {
                        return toSimilar(o, Math.max(text, 0.3), "Same service, reported within 6 hours");
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(SimilarIncident::score).reversed())
                .limit(8)
                .toList();
    }

    /** Resolutions from past incidents that look like this one. */
    public List<ResolutionSuggestion> suggestResolutions(Incident incident) {
        return incidentRepository.findResolvedWithNotes(
                        EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED))
                .stream()
                .filter(o -> !Objects.equals(o.getId(), incident.getId()))
                .map(o -> new ResolutionSuggestion(o.getId(), o.getIncidentNumber(), o.getTitle(),
                        o.getResolutionNotes(),
                        round(score(incident.getTitle(), incident.getDescription(), serviceId(incident),
                                incident.getCategory(), o))))
                .filter(s -> s.score() >= RELATED_THRESHOLD)
                .sorted(Comparator.comparingDouble(ResolutionSuggestion::score).reversed())
                .limit(3)
                .toList();
    }

    public double calculateSimilarity(String a, String b) {
        return TextSimilarity.jaccard(a, b);
    }

    private List<Incident> openIncidents() {
        return incidentRepository.findByStatusIn(IncidentStateFactory.openStatuses());
    }

    private static double score(String title, String description, Long serviceId, String category, Incident other) {
        boolean sameService = serviceId != null && Objects.equals(serviceId, serviceId(other));
        boolean sameCategory = category != null && category.equalsIgnoreCase(other.getCategory());
        return TextSimilarity.incidentScore(title, description, other.getTitle(), other.getDescription(),
                sameService, sameCategory);
    }

    private static Long serviceId(Incident i) {
        return i.getAffectedService() != null ? i.getAffectedService().getId() : null;
    }

    private static SimilarIncident toSimilar(Incident o, double score, String reason) {
        return new SimilarIncident(o.getId(), o.getIncidentNumber(), o.getTitle(), o.getStatus().name(),
                o.getAffectedService() != null ? o.getAffectedService().getName() : null,
                round(score), reason, o.getCreatedAt());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
