package com.itsm.repository;

import com.itsm.entity.AuditLog;
import com.itsm.entity.Incident;
import com.itsm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByIncidentOrderByTimestampAsc(Incident incident);

    List<AuditLog> findByPerformedByOrderByTimestampDesc(User user);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findTop200ByOrderByTimestampDesc();
}
