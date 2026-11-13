package com.itsm.repository;

import com.itsm.entity.Incident;
import com.itsm.entity.IncidentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentNoteRepository extends JpaRepository<IncidentNote, Long> {

    List<IncidentNote> findByIncidentOrderByCreatedAtDesc(Incident incident);
}
