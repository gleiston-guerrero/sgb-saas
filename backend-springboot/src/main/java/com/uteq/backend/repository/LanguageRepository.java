package com.uteq.backend.repository;

import com.uteq.backend.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {
    /**
     * Busca hasta cinco idiomas cuyo nombre contenga el texto, sin importar mayúsculas.
     *
     * @param name texto parcial del nombre de idioma.
     * @return idiomas coincidentes, con un máximo de cinco resultados.
     */
    List<Language> findTop5ByNameContainingIgnoreCase(String name);
    /**
     * Indica si ya existe un idioma con el nombre dado, sin importar mayúsculas.
     *
     * @param name nombre que se desea comprobar.
     * @return {@code true} si existe un idioma con ese nombre.
     */
    boolean existsByNameIgnoreCase(String name);
    /**
     * Indica si ya existe un idioma con el código dado, sin importar mayúsculas.
     *
     * @param code código que se desea comprobar.
     * @return {@code true} si existe un idioma con ese código.
     */
    boolean existsByCodeIgnoreCase(String code);
}
