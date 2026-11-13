package com.itsm.service;

import com.itsm.dto.DashboardStats;
import com.itsm.dto.ServiceAnalytics;
import com.itsm.dto.TechnicianWorkload;
import com.itsm.entity.ITService;
import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final ITServiceRepository itServiceRepository;

    public DashboardStats getDashboardStats() {
        List<Incident> all = incidentRepository.findAll();

        long total = all.size();
        long open = all.stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED &&
                        i.getStatus() != IncidentStatus.VERIFIED &&
                        i.getStatus() != IncidentStatus.CLOSED)
                .count();

        long critical = all.stream()
                .filter(i -> i.getPriority() == Priority.P1_CRITICAL)
                .count();

        long resolved = all.stream()
                .filter(i -> i.getStatus() == IncidentStatus.RESOLVED)
                .count();

        long closed = all.stream()
                .filter(i -> i.getStatus() == IncidentStatus.CLOSED)
                .count();

        LocalDateTime now = LocalDateTime.now();
        long violations = all.stream()
                .filter(i -> i.getResolutionDeadline() != null &&
                        i.getResolutionDeadline().isBefore(now) &&
                        i.getStatus() != IncidentStatus.RESOLVED &&
                        i.getStatus() != IncidentStatus.VERIFIED &&
                        i.getStatus() != IncidentStatus.CLOSED)
                .count();

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (IncidentCategory cat : IncidentCategory.values()) {
            long count = all.stream().filter(i -> i.getCategory() == cat).count();
            byCategory.put(cat.name(), count);
        }

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority prio : Priority.values()) {
            long count = all.stream().filter(i -> i.getPriority() == prio).count();
            byPriority.put(prio.name(), count);
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (IncidentStatus st : IncidentStatus.values()) {
            long count = all.stream().filter(i -> i.getStatus() == st).count();
            byStatus.put(st.name(), count);
        }

        return DashboardStats.builder()
                .totalIncidents(total)
                .openIncidents(open)
                .criticalIncidents(critical)
                .resolvedIncidents(resolved)
                .slaViolations(violations)
                .closedIncidents(closed)
                .incidentsByCategory(byCategory)
                .incidentsByPriority(byPriority)
                .incidentsByStatus(byStatus)
                .build();
    }

    public List<TechnicianWorkload> getTechnicianWorkloads() {
        List<TechnicianProfile> profiles = technicianProfileRepository.findAll();
        List<TechnicianWorkload> list = new ArrayList<>();

        for (TechnicianProfile p : profiles) {
            String name = p.getUser() != null ? p.getUser().getFullName() : "Unknown";
            Long userId = p.getUser() != null ? p.getUser().getId() : null;

            list.add(TechnicianWorkload.builder()
                    .technicianId(userId)
                    .technicianName(name)
                    .activeIncidents(p.getCurrentActiveIncidents())
                    .maxIncidents(p.getMaxActiveIncidents())
                    .totalResolved(p.getTotalResolvedIncidents())
                    .avgResolutionMinutes(p.getAvgResolutionTimeMinutes())
                    .skills(p.getSkills() != null ? p.getSkills() : Collections.emptyList())
                    .build());
        }

        return list;
    }

    public List<ServiceAnalytics> getServiceAnalytics() {
        List<ITService> services = itServiceRepository.findAll();
        List<ServiceAnalytics> analytics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (ITService s : services) {
            List<Incident> incidents = incidentRepository.findByAffectedService(s);

            long total = incidents.size();
            long open = incidents.stream()
                    .filter(i -> i.getStatus() != IncidentStatus.RESOLVED &&
                            i.getStatus() != IncidentStatus.VERIFIED &&
                            i.getStatus() != IncidentStatus.CLOSED)
                    .count();
            long resolved = incidents.stream()
                    .filter(i -> i.getStatus() == IncidentStatus.RESOLVED ||
                            i.getStatus() == IncidentStatus.CLOSED)
                    .count();
            long violations = incidents.stream()
                    .filter(i -> i.getResolutionDeadline() != null &&
                            i.getResolutionDeadline().isBefore(now) &&
                            i.getStatus() != IncidentStatus.RESOLVED &&
                            i.getStatus() != IncidentStatus.VERIFIED &&
                            i.getStatus() != IncidentStatus.CLOSED)
                    .count();

            analytics.add(ServiceAnalytics.builder()
                    .serviceId(s.getId())
                    .serviceName(s.getName())
                    .totalIncidents(total)
                    .openIncidents(open)
                    .resolvedIncidents(resolved)
                    .slaViolations(violations)
                    .build());
        }

        return analytics;
    }
}
