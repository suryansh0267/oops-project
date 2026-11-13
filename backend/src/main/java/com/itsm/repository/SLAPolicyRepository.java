package com.itsm.repository;

import com.itsm.enums.Priority;
import com.itsm.entity.SLAPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SLAPolicyRepository extends JpaRepository<SLAPolicy, Long> {

    Optional<SLAPolicy> findByPriority(Priority priority);

    List<SLAPolicy> findByActiveTrue();
}
