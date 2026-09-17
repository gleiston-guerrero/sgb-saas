package com.uteq.backend.repository;

import com.uteq.backend.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD del catálogo de autores más búsqueda predictiva por nombre.
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    /**
     * Busca hasta 5 autores cuyo nombre contenga el texto, sin importar mayúsculas.
     *
     * @param name texto a buscar dentro del nombre
     * @return lista de hasta 5 autores coincidentes
     */
    List<Author> findTop5ByNameContainingIgnoreCase(String name);
}
