package com.itsm.intelligence.frustration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface FrustrationRecordRepository extends JpaRepository<FrustrationRecord, Long> {
    List<FrustrationRecord> findByRecordedAtAfterOrderByRecordedAtAsc(Instant from);
}
