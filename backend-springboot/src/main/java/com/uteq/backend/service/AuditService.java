package com.uteq.backend.service;

import com.uteq.backend.dto.EventAuditResponseDTO;
import com.uteq.backend.dto.SummaryCategoryAuditDTO;
import com.uteq.backend.entity.AuditLogAudit;
import com.uteq.backend.entity.User;
import com.uteq.backend.repository.AuditLogAuditRepository;
import com.uteq.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consulta de {@code bitacora_auditoria} para GERENTE/ADMIN.
 */
@Service
public class AuditService {

    // Umbral para marcar "Revisar" en la categoría sesiones: 3 o más
    // LOGIN_FAIL en las últimas 24 horas.
    private static final long UMBRAL_LOGIN_FAIL_REVISAR = 3;

    private final AuditLogAuditRepository auditLogAuditRepo;
    private final UserRepository userRepo;

    /**
     * Constructor con los repositorios de auditoría y usuarios.
     *
     * @param auditLogAuditRepo repositorio de bitacora_auditoria
     * @param userRepo repositorio para resolver correos por id
     */
    public AuditService(AuditLogAuditRepository auditLogAuditRepo,
                             UserRepository userRepo) {
        this.auditLogAuditRepo = auditLogAuditRepo;
        this.userRepo = userRepo;
    }

    /**
     * Devuelve la página de eventos de {@code bitacora_auditoria} que cumplen los filtros dados.
     * Resuelve el correo de cada evento con una sola consulta por lote para evitar N+1 y lo expone
     * en el DTO junto al tipo de operación, la tabla afectada y la fecha.
     *
     * @param userId identificador del autor del evento; nulo incluye eventos de todos los usuarios
     * @param module nombre de la tabla afectada por la que se filtra; nulo desactiva ese filtro
     * @param from inicio del rango temporal de la consulta; nulo deja el rango sin cota inferior
     * @param until fin del rango temporal de la consulta; nulo deja el rango sin cota superior
     * @param pageable paginación, tamaño y orden solicitados por el panel de auditoría
     * @return página de eventos que coinciden con los filtros y la paginación solicitada
     */
    @Transactional(readOnly = true)
    public Page<EventAuditResponseDTO> list(Long userId, String module,
                                                     OffsetDateTime from, OffsetDateTime until,
                                                     Pageable pageable) {
        Page<AuditLogAudit> page = auditLogAuditRepo.searchWithFiltersCriteria(
                userId, module, from, until, pageable);

        // Resuelve correo por id con un solo IN (...) para evitar N+1.
        Set<Long> idsUsers = page.getContent().stream()
                .map(AuditLogAudit::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> emailById = userRepo.findAllById(idsUsers).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return page.map(event -> toDTO(event, emailById));
    }

    /**
     * Resume la bitácora por tabla afectada con totales históricos, eventos de hoy y última fecha.
     * Marca la categoría de sesiones para revisión cuando hay 3 o más inicios fallidos en 24 horas.
     *
     * @return resumen por categoría con totales, eventos de hoy, último evento y marca de revisión
     */
    @Transactional(readOnly = true)
    public List<SummaryCategoryAuditDTO> summary() {
        OffsetDateTime fromToday = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Object[]> rows = auditLogAuditRepo.summaryByCategory(fromToday);
        List<SummaryCategoryAuditDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            String tableAfectada = (String) row[0];
            long totalEvents = (Long) row[1];
            long eventsToday = (Long) row[2];
            OffsetDateTime lastEvent = row[3] instanceof OffsetDateTime odt ? odt : null;

            // TODO: definir criterio de "Revisar" cuando el equipo lo defina
            boolean requiereReview = false;
            if ("sesiones".equals(tableAfectada)) {
                long failsRecientes = auditLogAuditRepo.contarLoginFailRecientes(
                        OffsetDateTime.now().minusHours(24));
                requiereReview = failsRecientes >= UMBRAL_LOGIN_FAIL_REVISAR;
            }

            result.add(new SummaryCategoryAuditDTO(
                    tableAfectada, totalEvents, eventsToday, lastEvent, requiereReview));
        }

        return result;
    }

    /**
     * Genera el contenido CSV (UTF-8) de los eventos que cumplen los filtros, con cabecera
     * {@code id,usuarioId,tipoOperacion,tablaAfectada,fechaHora,detalles} y hasta 10000 filas
     * ordenadas por fecha descendente, escapando comas, saltos y comillas de cada celda.
     *
     * @param userId identificador del autor del evento; nulo incluye eventos de todos los usuarios
     * @param module nombre de la tabla afectada por la que se filtra; nulo desactiva ese filtro
     * @param from inicio del rango temporal incluido en la exportación; nulo deja sin cota inferior
     * @param until fin del rango temporal incluido en la exportación; nulo deja sin cota superior
     * @return bytes del CSV generado en UTF-8 listos para descargar
     */

    public byte[] exportCsv(Long userId, String module, OffsetDateTime from, OffsetDateTime until) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10000, org.springframework.data.domain.Sort.by("dateTime").descending());
        var page = auditLogAuditRepo.searchWithFiltersCriteria(userId, module, from, until, pageable);
        StringBuilder sb = new StringBuilder();
        sb.append("id,usuarioId,tipoOperacion,tablaAfectada,fechaHora,detalles\n");
        for (var e : page.getContent()) {
            sb.append(e.getId()).append(",").append(e.getUserId()).append(",").append(escape(e.getTypeOperacion())).append(",").append(escape(e.getTableAfectada())).append(",").append(e.getDateTime()).append(",").append(escape(e.getDetalles())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    private String escape(String s) { if (s==null) return ""; String t=s.replace("\"","\"\""); if (t.contains(",")||t.contains("\n")||t.contains("\"")) return "\""+t+"\""; return t; }

    private EventAuditResponseDTO toDTO(AuditLogAudit event, Map<Long, String> emailById) {
        String email = event.getUserId() == null ? null : emailById.get(event.getUserId());
        return new EventAuditResponseDTO(
                event.getId(),
                email,
                event.getTypeOperacion(),
                event.getDateTime(),
                event.getTableAfectada(),
                event.getDetalles());
    }
}
