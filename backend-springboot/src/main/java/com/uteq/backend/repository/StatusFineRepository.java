package com.uteq.backend.repository;

import com.uteq.backend.entity.StatusFine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusFineRepository extends JpaRepository<StatusFine, Integer> {

    /** Busca un estado de multa por su nombre exacto. */
    Optional<StatusFine> findByName(String name);
}
