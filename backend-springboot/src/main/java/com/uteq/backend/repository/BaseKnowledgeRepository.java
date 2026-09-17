package com.uteq.backend.repository;

import com.uteq.backend.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * CRUD de entradas de la base de conocimiento del chatbot.
 */
public interface BaseKnowledgeRepository extends JpaRepository<KnowledgeBase, Integer> {

    /**
     * Lista las entradas activas de la base de conocimiento.
     *
     * @return entradas con activo en verdadero
     */
    List<KnowledgeBase> findByActiveTrue();
}
