package com.itsm.repository;

import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.entity.ITService;
import com.itsm.entity.Incident;
import com.itsm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByReporter(User reporter);

    List<Incident> findByAssignedTechnician(User technician);

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByPriority(Priority priority);

    List<Incident> findByCategory(IncidentCategory category);

    List<Incident> findByAffectedService(ITService service);

    List<Incident> findByStatusIn(List<IncidentStatus> statuses);

    long countByStatus(IncidentStatus status);

    long countByPriority(Priority priority);

    long countByAssignedTechnician(User technician);

    long countByAssignedTechnicianAndStatusIn(User technician, List<IncidentStatus> statuses);

    @Query("SELECT i FROM Incident i WHERE i.resolutionDeadline < :now AND i.status NOT IN ('RESOLVED','VERIFIED','CLOSED')")
    List<Incident> findSLAViolations(@Param("now") LocalDateTime now);

    @Query("SELECT i FROM Incident i WHERE i.resolutionDeadline BETWEEN :now AND :deadline AND i.status NOT IN ('RESOLVED','VERIFIED','CLOSED')")
    List<Incident> findApproachingSLADeadlines(@Param("now") LocalDateTime now, @Param("deadline") LocalDateTime deadline);

    List<Incident> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT i FROM Incident i WHERE LOWER(i.title) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%',:keyword,'%'))")
    List<Incident> searchByKeyword(@Param("keyword") String keyword);
}
