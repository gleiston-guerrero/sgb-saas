package com.uteq.backend.repository;

import com.uteq.backend.entity.AuditLogAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Filtro paginado de bitácora vía Criteria API (P5).
 *
 * <p>Reemplaza la query nativa con CASTs para parámetros NULL (bug
 * PostgreSQL + NULL en JPQL): los predicados se agregan solo cuando el
 * filtro es no-nulo — paridad exacta con las guardas NULL del SQL
 * original (módulo vacío filtra literal, igual que antes). El orden usa propiedades de entidad; el nombre
 * físico histórico {@code fecha_hora} se traduce a {@code dateTime} por
 * compatibilidad con llamadores antiguos.
 */
public interface AuditLogAuditRepositoryCustom {

    /**
     * Filtra la bitácora por usuario, módulo y rango de fechas, paginado.
     *
     * @param userId autor del evento, nulo = todos
     * @param module tabla afectada, nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @param pageable paginación y orden con propiedades de entidad
     * @return página de eventos coincidentes
     */
    Page<AuditLogAudit> searchWithFiltersCriteria(Long userId, String module,
            OffsetDateTime from, OffsetDateTime until, Pageable pageable);
}
