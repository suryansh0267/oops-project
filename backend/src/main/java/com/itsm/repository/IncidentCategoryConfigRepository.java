package com.itsm.repository;

import com.itsm.entity.IncidentCategoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentCategoryConfigRepository extends JpaRepository<IncidentCategoryConfig, Long> {
    Optional<IncidentCategoryConfig> findByNameIgnoreCase(String name);
    List<IncidentCategoryConfig> findByActiveTrue();
    boolean existsByNameIgnoreCase(String name);
}
