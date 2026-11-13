package com.itsm.repository;

import com.itsm.entity.TechnicianProfile;
import com.itsm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile, Long> {

    Optional<TechnicianProfile> findByUser(User user);

    List<TechnicianProfile> findByAvailableTrue();

    @Query("SELECT tp FROM TechnicianProfile tp WHERE tp.available = true AND tp.currentActiveIncidents < tp.maxActiveIncidents")
    List<TechnicianProfile> findAvailableTechnicians();

    @Query("SELECT tp FROM TechnicianProfile tp WHERE tp.available = true AND tp.currentActiveIncidents < tp.maxActiveIncidents AND :skill MEMBER OF tp.skills")
    List<TechnicianProfile> findBySkill(@Param("skill") String skill);
}
