package com.itsm.repository;

import com.itsm.entity.ITService;
import com.itsm.entity.Incident;
import com.itsm.entity.User;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByIncidentNumberIgnoreCase(String incidentNumber);

    List<Incident> findByReporterOrderByCreatedAtDesc(User reporter);

    List<Incident> findByAssignedTechnicianOrderByCreatedAtDesc(User technician);

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByPriority(Priority priority);

    List<Incident> findByAffectedService(ITService service);

    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);

    long countByAssignedTechnicianAndStatusIn(User technician, Collection<IncidentStatus> statuses);

    long countByAssignedTechnicianAndAffectedServiceAndStatusIn(User technician, ITService service,
                                                                Collection<IncidentStatus> statuses);

    List<Incident> findByStatusInAndCreatedAtAfter(Collection<IncidentStatus> statuses, LocalDateTime after);

    /** Resolved incidents that carry resolution notes, for "suggested resolution". */
    @Query("SELECT i FROM Incident i WHERE i.status IN :statuses AND i.resolutionNotes IS NOT NULL")
    List<Incident> findResolvedWithNotes(@Param("statuses") Collection<IncidentStatus> statuses);

    List<Incident> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
