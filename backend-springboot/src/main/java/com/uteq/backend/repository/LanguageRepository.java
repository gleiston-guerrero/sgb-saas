package com.uteq.backend.repository;

import com.uteq.backend.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {
    /** Busca hasta 5 idiomas cuyo nombre contenga el texto, sin importar mayúsculas. */
    List<Language> findTop5ByNameContainingIgnoreCase(String name);
    /** Indica si ya existe un idioma con el nombre dado, sin importar mayúsculas. */
    boolean existsByNameIgnoreCase(String name);
    /** Indica si ya existe un idioma con el código dado, sin importar mayúsculas. */
    boolean existsByCodeIgnoreCase(String code);
}