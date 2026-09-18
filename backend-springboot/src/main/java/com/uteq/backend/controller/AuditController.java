package com.uteq.backend.controller;

import com.uteq.backend.dto.EventAuditResponseDTO;
import com.uteq.backend.dto.SummaryCategoryAuditDTO;
import com.uteq.backend.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consulta de bitácora de auditoría, solo GERENTE/ADMIN.
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
public class AuditController {

    private final AuditService auditService;

    /**
     * Constructor con el servicio de auditoría.
     *
     * @param auditService servicio de consulta de bitacora_auditoria
     */
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // ── GET /api/v1/auditoria?usuarioId=&modulo=&desde=&hasta= ──
    /**
     * Lista la bitácora de auditoría con filtros opcionales por usuario, módulo y rango de fechas.
     * Solo GERENTE y ADMIN. Delega en AuditService y devuelve la página de eventos.
     *
     * @param userId id de usuario a filtrar, null para todos
     * @param module nombre del módulo a filtrar, null para todos
     * @param from fecha y hora inicial del rango, null sin límite inferior
     * @param until fecha y hora final del rango, null sin límite superior
     * @param pageable paginación y orden solicitados
     * @return página de eventos de auditoría que cumplen los filtros
     */
    @GetMapping
    public ResponseEntity<Page<EventAuditResponseDTO>> list(
            @RequestParam(name = "usuarioId", required = false) Long userId,
            @RequestParam(name = "modulo", required = false) String module,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until,
            // sort con propiedad de entidad (dateTime): el repositorio es
            // Criteria (P5) y traduce el histórico "fecha_hora" por
            // compatibilidad, pero el default ya usa el nombre vigente.
            @PageableDefault(size = 20, sort = "dateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                auditService.list(userId, module, from, until, pageable));
    }

    // ── GET /api/v1/auditoria/resumen ────────────────────────
    // Agregación por tabla_afectada: total, hoy, último evento.
    // Misma restricción @PreAuthorize que el listado (GERENTE/ADMIN).
    /**
     * Devuelve el resumen de auditoría agrupado por tabla afectada con totales y último evento.
     * Solo GERENTE y ADMIN. Delega en AuditService.
     *
     * @return lista con el resumen por tabla afectada
     */
    @GetMapping("/resumen")
    public ResponseEntity<List<SummaryCategoryAuditDTO>> summary() {
        return ResponseEntity.ok(auditService.summary());
    }
    /**
     * Descarga la bitácora filtrada como archivo CSV adjunto.
     * Solo GERENTE y ADMIN. Aplica los mismos filtros que el listado.
     *
     * @param format formato solicitado, actualmente solo se genera CSV
     * @param userId id de usuario a filtrar, null para todos
     * @param module nombre del módulo a filtrar, null para todos
     * @param from fecha y hora inicial del rango, null sin límite inferior
     * @param until fecha y hora final del rango, null sin límite superior
     * @return bytes del CSV con la cabecera de descarga correspondiente
     */

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(name = "formato", defaultValue = "csv") String format,
                                         @RequestParam(name = "usuarioId", required = false) Long userId,
                                         @RequestParam(name = "modulo", required = false) String module,
                                         @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                         @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        byte[] data = auditService.exportCsv(userId, module, from, until);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria.csv").contentType(MediaType.parseMediaType("text/csv")).contentLength(data.length).body(data);
    }
}
