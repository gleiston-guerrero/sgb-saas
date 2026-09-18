package com.uteq.backend.repository;

import com.uteq.backend.entity.BackupSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * CRUD de programaciones de respaldo.
 */
@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Long> {

    /**
     * Lista programaciones activas ordenadas por última ejecución.
     *
     * @return programaciones activas, la de ejecución más reciente primero
     */
    java.util.List<BackupSchedule> findByActiveTrueOrderByLastExecutionDesc();

    /**
     * Busca una programación por ID solo si está activa.
     *
     * @param id identificador de la programación
     * @return la programación activa o null si no existe
     */
    BackupSchedule findByIdAndActiveTrue(Long id);
}