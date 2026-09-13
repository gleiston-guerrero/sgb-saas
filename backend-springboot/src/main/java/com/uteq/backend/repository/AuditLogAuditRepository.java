package com.uteq.backend.repository;

import com.uteq.backend.entity.AuditLogAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * CRUD de {@code bitacora_auditoria} más filtro paginado (query nativa con casts para parámetros NULL).
 */
@Repository
public interface AuditLogAuditRepository extends JpaRepository<AuditLogAudit, Long> {

    // Sin ORDER BY interno: el orden lo inyecta Spring Data desde el
    // Pageable del controller (@PageableDefault sort="fecha_hora"), que en
    // queries nativas pasa el nombre de columna TAL CUAL -- por eso el sort
    // debe usar el nombre fisico (fecha_hora), no la propiedad (fechaHora).
    // Se evaluo convertir a JPQL (P4/nativeQuery): AuditService.java linea
    // ~113 construye el Pageable con Sort.by("fecha_hora") -- si esta query
    // fuera JPQL, Spring Data validaria ese "fecha_hora" contra las
    // propiedades de la entidad (dateTime, no fecha_hora) y fallaria en
    // runtime. Se deja nativa para no arriesgar ese sort real; convertirla
    // exigiria tambien cambiar el Sort del llamador, fuera del alcance de
    // un cambio de bajo riesgo.
    @Query(value = "SELECT * FROM bitacora_auditoria b WHERE "
            + "(CAST(:userId AS bigint) IS NULL OR b.usuario_id = CAST(:userId AS bigint)) AND "
            + "(CAST(:module AS text) IS NULL OR b.tabla_afectada = CAST(:module AS text)) AND "
            + "(CAST(:from AS timestamptz) IS NULL OR b.fecha_hora >= CAST(:from AS timestamptz)) AND "
            + "(CAST(:until AS timestamptz) IS NULL OR b.fecha_hora <= CAST(:until AS timestamptz))",
           countQuery = "SELECT count(*) FROM bitacora_auditoria b WHERE "
            + "(CAST(:userId AS bigint) IS NULL OR b.usuario_id = CAST(:userId AS bigint)) AND "
            + "(CAST(:module AS text) IS NULL OR b.tabla_afectada = CAST(:module AS text)) AND "
            + "(CAST(:from AS timestamptz) IS NULL OR b.fecha_hora >= CAST(:from AS timestamptz)) AND "
            + "(CAST(:until AS timestamptz) IS NULL OR b.fecha_hora <= CAST(:until AS timestamptz))",
           nativeQuery = true)
    Page<AuditLogAudit> searchWithFilters(
            @Param("userId") Long userId,
            @Param("module") String module,
            @Param("from") OffsetDateTime from,
            @Param("until") OffsetDateTime until,
            Pageable pageable);

    // Resumen por categoría: una sola query de agregación en vez de 8
    // llamadas al listado paginado. Devuelve Object[] porque la query
    // no mapea directamente a un record -- el service transforma a
    // ResumenCategoriaAuditoriaDTO. Migrada de nativeQuery a JPQL (P4):
    // el COUNT(*) FILTER (WHERE ...) de PostgreSQL no tiene equivalente en
    // JPQL/HQL, pero SUM(CASE WHEN ... THEN 1L ELSE 0L END) es una
    // agregacion condicional estandar y produce el mismo resultado sin SQL
    // especifico de un motor.
    @Query("SELECT b.tableAfectada, "
            + "COUNT(b), "
            + "SUM(CASE WHEN b.dateTime >= :fromToday THEN 1L ELSE 0L END), "
            + "MAX(b.dateTime) "
            + "FROM AuditLogAudit b "
            + "GROUP BY b.tableAfectada "
            + "ORDER BY b.tableAfectada")
    List<Object[]> summaryByCategory(@Param("fromToday") OffsetDateTime fromToday);

    // Login fallidos en las últimas 24h (para decidir "Revisar" en sesiones).
    // Migrada de nativeQuery a JPQL (P4): sin sintaxis especifica de motor.
    @Query("SELECT COUNT(b) FROM AuditLogAudit b "
            + "WHERE b.tableAfectada = 'sesiones' "
            + "AND b.typeOperacion = 'LOGIN_FAIL' "
            + "AND b.dateTime >= :from")
    long contarLoginFailRecientes(@Param("from") OffsetDateTime from);
}
