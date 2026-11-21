package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentStrategyManager extends StrategyRegistry<AssignmentStrategy> {

    public AssignmentStrategyManager(List<AssignmentStrategy> strategies) {
        super(strategies, "skillBased");
    }

    public Optional<TechnicianProfile> assign(Incident incident, List<TechnicianProfile> technicians) {
        return active().assignTechnician(incident, technicians);
    }
}
