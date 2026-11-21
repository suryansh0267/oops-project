package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component("loadBased")
public class LoadBasedAssignmentStrategy implements AssignmentStrategy {

    @Override
    public String getKey() {
        return "loadBased";
    }

    @Override
    public String getDisplayName() {
        return "Load-based";
    }

    @Override
    public String getDescription() {
        return "Gives the incident to the available technician with the fewest active incidents.";
    }

    @Override
    public Optional<TechnicianProfile> assignTechnician(Incident incident, List<TechnicianProfile> availableTechnicians) {
        if (availableTechnicians == null || availableTechnicians.isEmpty()) {
            return Optional.empty();
        }

        // Pick the technician with the lowest current active incidents
        return availableTechnicians.stream()
                .min(Comparator.comparingInt(TechnicianProfile::getCurrentActiveIncidents));
    }
}
