package com.uteq.backend.repository;

import com.uteq.backend.entity.TypeDamage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeDamageRepository extends JpaRepository<TypeDamage, Integer> {

    /** Lista los tipos de daño activos. */
    List<TypeDamage> findByActiveTrue();

    /** Busca un tipo de daño por su nombre exacto. */
    Optional<TypeDamage> findByName(String name);

    /** Lista los tipos de daño activos de la categoría dada. */
    List<TypeDamage> findByActiveTrueAndCategoryId(Integer categoryId);
}
