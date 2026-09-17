package com.uteq.backend.repository;

import com.uteq.backend.entity.CategoryDamage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CRUD del catálogo de categorías de daño.
 */
@Repository
public interface CategoryDamageRepository extends JpaRepository<CategoryDamage, Integer> {
    /**
     * Busca una categoría de daño por su nombre exacto.
     *
     * @param name nombre a buscar
     * @return la categoría si existe
     */
    Optional<CategoryDamage> findByName(String name);
}
