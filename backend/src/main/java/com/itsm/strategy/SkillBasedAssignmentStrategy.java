package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component("skillBased")
public class SkillBasedAssignmentStrategy implements AssignmentStrategy {

    @Override
    public Optional<TechnicianProfile> assignTechnician(Incident incident, List<TechnicianProfile> availableTechnicians) {
        if (availableTechnicians == null || availableTechnicians.isEmpty()) {
            return Optional.empty();
        }

        String categoryName = incident.getCategory() != null
                ? incident.getCategory().name().toLowerCase()
                : "";

        // Filter technicians whose skills match the incident category
        List<TechnicianProfile> matchedTechnicians = availableTechnicians.stream()
                .filter(tech -> tech.getSkills() != null && tech.getSkills().stream()
                        .anyMatch(skill -> skill.equalsIgnoreCase(categoryName)))
                .toList();

        if (matchedTechnicians.isEmpty()) {
            return Optional.empty();
        }

        // Among matched technicians, pick the one with the lowest workload
        return matchedTechnicians.stream()
                .min(Comparator.comparingInt(TechnicianProfile::getCurrentActiveIncidents));
    }
}
