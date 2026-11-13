package com.itsm.repository;

import com.itsm.entity.ITService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITServiceRepository extends JpaRepository<ITService, Long> {

    Optional<ITService> findByName(String name);

    List<ITService> findByActiveTrue();

    List<ITService> findByCategory(String category);
}
