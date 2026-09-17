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
 * CRUD de {@code bitacora_auditoria} más filtro paginado (Criteria API, P5).
 */
@Repository
public interface AuditLogAuditRepository
        extends JpaRepository<AuditLogAudit, Long>, AuditLogAuditRepositoryCustom {


    // Resumen por categoría: una sola query de agregación en vez de 8
    // llamadas al listado paginado. Devuelve Object[] porque la query
    // no mapea directamente a un record -- el service transforma a
    // ResumenCategoriaAuditoriaDTO. Migrada de nativeQuery a JPQL (P4):
    // el COUNT(*) FILTER (WHERE ...) de PostgreSQL no tiene equivalente en
    // JPQL/HQL, pero SUM(CASE WHEN ... THEN 1L ELSE 0L END) es una
    // agregacion condicional estandar y produce el mismo resultado sin SQL
    // especifico de un motor.
    /**
     * Resume eventos por categoría con totales y actividad de hoy.
     *
     * @param fromToday inicio del día actual para el conteo de hoy
     * @return filas [tablaAfectada, total, hoy, último evento]
     */
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
    /**
     * Cuenta los logins fallidos desde la fecha dada.
     *
     * @param from inicio del rango a contar
     * @return cantidad de LOGIN_FAIL desde esa fecha
     */
    @Query("SELECT COUNT(b) FROM AuditLogAudit b "
            + "WHERE b.tableAfectada = 'sesiones' "
            + "AND b.typeOperacion = 'LOGIN_FAIL' "
            + "AND b.dateTime >= :from")
    long contarLoginFailRecientes(@Param("from") OffsetDateTime from);
}
