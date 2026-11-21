package com.itsm.service;

import com.itsm.dto.TechnicianRecommendation;
import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.intelligence.TechnicianScorer;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/** Smart technician recommendation (intelligent feature D). */
@Service
@RequiredArgsConstructor
public class TechnicianRecommendationService {

    private static final EnumSet<IncidentStatus> DONE =
            EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);

    private final TechnicianProfileRepository technicianProfileRepository;
    private final IncidentRepository incidentRepository;

    /** Every technician ranked best-first, with the reasons behind the score. */
    public List<TechnicianRecommendation> rank(Incident incident) {
        return technicianProfileRepository.findAll().stream()
                .filter(p -> p.getUser() != null && p.getUser().isActive())
                .map(p -> new TechnicianRecommendation(
                        p.getUser().getId(), p.getUser().getFullName(), p.getSkills(),
                        p.getCurrentActiveIncidents(), p.getMaxActiveIncidents(),
                        p.isAvailable() && p.getCurrentActiveIncidents() < p.getMaxActiveIncidents(),
                        score(incident, p)))
                .sorted(Comparator.comparing(TechnicianRecommendation::available).reversed()
                        .thenComparing(r -> -r.score().total()))
                .toList();
    }

    public Optional<TechnicianProfile> best(Incident incident, List<TechnicianProfile> candidates) {
        return candidates.stream().max(Comparator.comparingDouble(p -> score(incident, p).total()));
    }

    public TechnicianScorer.Score score(Incident incident, TechnicianProfile p) {
        String category = incident.getCategory();
        boolean skillMatch = category != null && p.getSkills() != null
                && p.getSkills().stream().anyMatch(s -> s.equalsIgnoreCase(category));
        long sameService = incident.getAffectedService() == null ? 0
                : incidentRepository.countByAssignedTechnicianAndAffectedServiceAndStatusIn(
                        p.getUser(), incident.getAffectedService(), DONE);
        boolean critical = incident.getPriority() == Priority.P1_CRITICAL || incident.getPriority() == Priority.P2_HIGH;
        return TechnicianScorer.score(skillMatch, p.getCurrentActiveIncidents(), p.getMaxActiveIncidents(),
                p.getTotalResolvedIncidents(), sameService, critical);
    }
}
