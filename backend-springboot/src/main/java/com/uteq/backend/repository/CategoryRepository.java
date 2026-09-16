package com.uteq.backend.repository;

import com.uteq.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD del catálogo de categorías más búsqueda predictiva y existencia.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    /**
     * Busca hasta 5 categorías cuyo nombre contenga el texto.
     *
     * @param name texto a buscar dentro del nombre
     * @return lista de hasta 5 categorías coincidentes
     */
    List<Category> findTop5ByNameContainingIgnoreCase(String name);
    /**
     * Indica si ya existe una categoría con el nombre dado.
     *
     * @param name nombre a verificar
     * @return verdadero si existe
     */
    boolean existsByNameIgnoreCase(String name);
}
