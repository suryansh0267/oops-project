package com.itsm.service;

import com.itsm.dto.DashboardStats;
import com.itsm.dto.ServiceAnalytics;
import com.itsm.dto.TechnicianWorkload;
import com.itsm.entity.ITService;
import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.repository.TechnicianProfileRepository;
import com.itsm.sla.SlaStatus;
import com.itsm.state.IncidentStateFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    /** This many open incidents on one service within an hour suggests an outage. */
    static final int OUTAGE_THRESHOLD = 3;
    static final Duration OUTAGE_WINDOW = Duration.ofHours(1);

    private final IncidentRepository incidentRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final ITServiceRepository itServiceRepository;
    private final SlaViolationRepository slaViolationRepository;
    private final IncidentMapper mapper;

    public DashboardStats getDashboardStats() {
        List<Incident> all = incidentRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        Predicate<Incident> open = i -> IncidentStateFactory.of(i.getStatus()).isOpen();

        Map<SlaStatus, Long> sla = all.stream()
                .collect(Collectors.groupingBy(i -> mapper.slaStatus(i, now), () -> new EnumMap<>(SlaStatus.class),
                        Collectors.counting()));
        long met = sla.getOrDefault(SlaStatus.MET, 0L);
        long missed = sla.getOrDefault(SlaStatus.MISSED, 0L);

        Map<String, Long> byCategory = all.stream()
                .collect(Collectors.groupingBy(i -> i.getCategory() == null ? "OTHER" : i.getCategory(),
                        TreeMap::new, Collectors.counting()));
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            byPriority.put(p.name(), all.stream().filter(i -> i.getPriority() == p).count());
        }
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (IncidentStatus s : IncidentStatus.values()) {
            byStatus.put(s.name(), all.stream().filter(i -> i.getStatus() == s).count());
        }

        return DashboardStats.builder()
                .totalIncidents(all.size())
                .openIncidents(all.stream().filter(open).count())
                .criticalIncidents(all.stream().filter(open).filter(i -> i.getPriority() == Priority.P1_CRITICAL).count())
                .resolvedIncidents(all.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED
                        || i.getStatus() == IncidentStatus.VERIFIED).count())
                .closedIncidents(all.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED).count())
                .slaViolations(sla.getOrDefault(SlaStatus.BREACHED, 0L))
                .slaAtRisk(sla.getOrDefault(SlaStatus.AT_RISK, 0L))
                .recordedViolations(slaViolationRepository.count())
                .slaCompliancePercent(met + missed == 0 ? 100.0 : round(100.0 * met / (met + missed)))
                .meanTimeToResolveMinutes(round(all.stream()
                        .filter(i -> i.getResolvedAt() != null && i.getCreatedAt() != null)
                        .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes())
                        .average().orElse(0)))
                .meanTimeToRespondMinutes(round(all.stream()
                        .filter(i -> i.getRespondedAt() != null && i.getCreatedAt() != null)
                        .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getRespondedAt()).toMinutes())
                        .average().orElse(0)))
                .averageRating(round(all.stream().filter(i -> i.getFeedbackRating() > 0)
                        .mapToInt(Incident::getFeedbackRating).average().orElse(0)))
                .unassignedIncidents(all.stream().filter(open).filter(i -> i.getAssignedTechnician() == null).count())
                .incidentsByCategory(byCategory)
                .incidentsByPriority(byPriority)
                .incidentsByStatus(byStatus)
                .dailyTrend(trend(all, 14))
                .build();
    }

    private List<Map<String, Object>> trend(List<Incident> all, int days) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> out = new ArrayList<>();
        for (int d = days - 1; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            long created = all.stream().filter(i -> i.getCreatedAt() != null
                    && i.getCreatedAt().toLocalDate().equals(day)).count();
            long resolved = all.stream().filter(i -> i.getResolvedAt() != null
                    && i.getResolvedAt().toLocalDate().equals(day)).count();
            out.add(Map.of("date", day.toString(), "created", created, "resolved", resolved));
        }
        return out;
    }

    public List<TechnicianWorkload> getTechnicianWorkloads() {
        List<TechnicianWorkload> list = new ArrayList<>();
        for (TechnicianProfile p : technicianProfileRepository.findAll()) {
            list.add(toWorkload(p));
        }
        list.sort(Comparator.comparingInt(TechnicianWorkload::getActiveIncidents).reversed());
        return list;
    }

    public TechnicianWorkload toWorkload(TechnicianProfile p) {
        return TechnicianWorkload.builder()
                .technicianId(p.getUser() != null ? p.getUser().getId() : null)
                .technicianName(p.getUser() != null ? p.getUser().getFullName() : "Unknown")
                .activeIncidents(p.getCurrentActiveIncidents())
                .maxIncidents(p.getMaxActiveIncidents())
                .totalResolved(p.getTotalResolvedIncidents())
                .avgResolutionMinutes(round(p.getAvgResolutionTimeMinutes()))
                .available(p.isAvailable())
                .skills(p.getSkills() != null ? p.getSkills() : Collections.emptyList())
                .build();
    }

    public List<ServiceAnalytics> getServiceAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        List<ServiceAnalytics> analytics = new ArrayList<>();
        for (ITService s : itServiceRepository.findAll()) {
            List<Incident> incidents = incidentRepository.findByAffectedService(s);
            analytics.add(ServiceAnalytics.builder()
                    .serviceId(s.getId())
                    .serviceName(s.getName())
                    .businessCriticality(s.getBusinessCriticality())
                    .totalIncidents(incidents.size())
                    .openIncidents(incidents.stream().filter(i -> IncidentStateFactory.of(i.getStatus()).isOpen()).count())
                    .resolvedIncidents(incidents.stream().filter(i -> !IncidentStateFactory.of(i.getStatus()).isOpen()).count())
                    .slaViolations(incidents.stream().filter(i -> {
                        SlaStatus st = mapper.slaStatus(i, now);
                        return st == SlaStatus.BREACHED || st == SlaStatus.MISSED;
                    }).count())
                    .build());
        }
        analytics.sort(Comparator.comparingLong(ServiceAnalytics::getTotalIncidents).reversed());
        return analytics;
    }

    /**
     * Correlation at service level: several open incidents on the same service within an hour
     * usually mean one outage, so admins see a single alert instead of N tickets.
     */
    public List<Map<String, Object>> getServiceAlerts() {
        LocalDateTime since = LocalDateTime.now().minus(OUTAGE_WINDOW);
        Map<ITService, List<Incident>> recent = incidentRepository
                .findByStatusInAndCreatedAtAfter(IncidentStateFactory.openStatuses(), since).stream()
                .filter(i -> i.getAffectedService() != null)
                .collect(Collectors.groupingBy(Incident::getAffectedService));
        List<Map<String, Object>> alerts = new ArrayList<>();
        recent.forEach((service, incidents) -> {
            if (incidents.size() >= OUTAGE_THRESHOLD) {
                alerts.add(Map.of(
                        "serviceId", service.getId(),
                        "serviceName", service.getName(),
                        "count", incidents.size(),
                        "incidentNumbers", incidents.stream().map(Incident::getIncidentNumber).toList(),
                        "message", incidents.size() + " reports in the last hour. Likely a single outage."));
            }
        });
        return alerts;
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
