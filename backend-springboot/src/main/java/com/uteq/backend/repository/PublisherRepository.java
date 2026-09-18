package com.uteq.backend.repository;

import com.uteq.backend.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Integer> {
    /** Busca hasta 5 editoriales cuyo nombre contenga el texto, sin importar mayúsculas. */
    List<Publisher> findTop5ByNameContainingIgnoreCase(String name);
    /** Indica si ya existe una editorial con el nombre dado, sin importar mayúsculas. */
    boolean existsByNameIgnoreCase(String name);
}