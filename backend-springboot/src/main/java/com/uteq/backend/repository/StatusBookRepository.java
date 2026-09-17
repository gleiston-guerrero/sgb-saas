package com.uteq.backend.repository;

import com.uteq.backend.entity.StatusBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusBookRepository extends JpaRepository<StatusBook, Integer> {

    /** Busca un estado de libro por su nombre exacto. */
    Optional<StatusBook> findByName(String name);
}