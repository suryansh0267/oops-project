package com.itsm.repository;

import com.itsm.entity.Incident;
import com.itsm.entity.SlaViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlaViolationRepository extends JpaRepository<SlaViolation, Long> {

    boolean existsByIncidentAndViolationType(Incident incident, SlaViolation.Type type);

    List<SlaViolation> findTop200ByOrderByDetectedAtDesc();

    List<SlaViolation> findByIncidentOrderByDetectedAtAsc(Incident incident);

    long count();
}
