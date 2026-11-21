package com.itsm.intelligence.frustration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FrustrationStateRepository extends JpaRepository<FrustrationState, String> {
    List<FrustrationState> findByIncidentId(Long incidentId);
}
