package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;

import java.util.List;
import java.util.Optional;

public interface AssignmentStrategy extends NamedStrategy {

    Optional<TechnicianProfile> assignTechnician(Incident incident, List<TechnicianProfile> availableTechnicians);
}
