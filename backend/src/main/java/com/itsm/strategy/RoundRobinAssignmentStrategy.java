package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component("roundRobin")
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String getKey() {
        return "roundRobin";
    }

    @Override
    public String getDisplayName() {
        return "Round-robin";
    }

    @Override
    public String getDescription() {
        return "Rotates through available technicians in turn so work is spread evenly.";
    }

    @Override
    public Optional<TechnicianProfile> assignTechnician(Incident incident, List<TechnicianProfile> availableTechnicians) {
        if (availableTechnicians == null || availableTechnicians.isEmpty()) {
            return Optional.empty();
        }

        int index = Math.abs(counter.getAndIncrement() % availableTechnicians.size());
        return Optional.of(availableTechnicians.get(index));
    }
}
