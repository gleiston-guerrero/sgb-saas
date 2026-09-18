package com.uteq.backend.controller;

import com.uteq.backend.dto.ChangeStatusSuggestionRequestDTO;
import com.uteq.backend.dto.SuggestionAcquisitionRequestDTO;
import com.uteq.backend.dto.SuggestionAcquisitionResponseDTO;
import com.uteq.backend.dto.SuggestionGroupedDTO;
import com.uteq.backend.service.ReportPdfService;
import com.uteq.backend.service.SuggestionAcquisitionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Lector crea, gerente lista y cambia estado. LECTOR ve solo las
// suyas; GERENTE/ADMIN ven todas.
@RestController
@RequestMapping("/api/v1/sugerencias-adquisicion")
public class SuggestionAcquisitionController {

    private final SuggestionAcquisitionService suggestionService;
    private final ReportPdfService reportPdfService;

    /**
     * Constructor con el servicio de sugerencias y el generador de reportes PDF.
     *
     * @param suggestionService servicio de creación y gestión de sugerencias de adquisición
     * @param reportPdfService servicio de generación de reportes en PDF
     */
    public SuggestionAcquisitionController(SuggestionAcquisitionService suggestionService,
                                           ReportPdfService reportPdfService) {
        this.suggestionService = suggestionService;
        this.reportPdfService = reportPdfService;
    }

    // ── POST /api/v1/sugerencias-adquisicion ──────────────
    /**
     * Crea una sugerencia de adquisición de un libro para el propio LECTOR. Solo LECTOR.
     *
     * @param dto título, autor e ISBN del libro sugerido
     * @param authentication identidad del LECTOR que sugiere la compra
     * @return sugerencia creada con estado 201
     */
    @PostMapping
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<SuggestionAcquisitionResponseDTO> create(
            @Valid @RequestBody SuggestionAcquisitionRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(suggestionService.create(dto, authentication));
    }

    // ── GET /api/v1/sugerencias-adquisicion/mias ──────────
    /**
     * Lista en forma paginada las sugerencias del propio LECTOR autenticado. Solo LECTOR.
     *
     * @param authentication identidad del LECTOR cuyas sugerencias se consultan
     * @param pageable paginación y orden solicitados
     * @return página con las sugerencias del lector
     */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<Page<SuggestionAcquisitionResponseDTO>> listOwns(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "created") Pageable pageable) {
        return ResponseEntity.ok(suggestionService.listOwns(authentication, pageable));
    }

    // ── GET /api/v1/sugerencias-adquisicion?estado=PENDIENTE ──
    /**
     * Lista en forma paginada todas las sugerencias, opcionalmente filtradas por estado.
     * Solo GERENTE y ADMIN.
     *
     * @param status estado a filtrar como PENDIENTE, null para todos
     * @param pageable paginación y orden solicitados
     * @return página con las sugerencias que cumplen el filtro
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<SuggestionAcquisitionResponseDTO>> listAll(
            @RequestParam(name = "estado", required = false) String status,
            @PageableDefault(size = 10, sort = "created") Pageable pageable) {
        return ResponseEntity.ok(suggestionService.listAll(status, pageable));
    }

    // ── PATCH /api/v1/sugerencias-adquisicion/{id}/estado ─
    /**
     * Aprueba o rechaza una sugerencia de adquisición registrando al revisor. Solo GERENTE y ADMIN.
     *
     * @param id id de la sugerencia a resolver
     * @param dto estado nuevo de la sugerencia
     * @param authentication identidad del gerente que resuelve la sugerencia
     * @return sugerencia actualizada con su estado nuevo
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<SuggestionAcquisitionResponseDTO> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusSuggestionRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(
                suggestionService.changeStatus(id, dto.freshStatus(), authentication));
    }

    // ── GET /api/v1/sugerencias-adquisicion/mas-pedidos ──
    // Gestión por demanda (GERENTE/ADMIN): PENDIENTE agrupadas por ISBN.
    // Importante: va ANTES de que alguien agregue un @GetMapping("/{id}")
    // para que "mas-pedidos" no se confunda con un id.
    /**
     * Devuelve en forma paginada las sugerencias pendientes agrupadas por ISBN para compra por demanda.
     * Solo GERENTE y ADMIN.
     *
     * @param pageable paginación solicitada
     * @return página de ISBN sugeridos con su conteo de pedidos
     */
    @GetMapping("/mas-pedidos")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<SuggestionGroupedDTO>> mostRequested(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(suggestionService.getMostRequested(pageable));
    }

    // ── POST /api/v1/sugerencias-adquisicion/confirmar-adquisicion?isbn= ──
    // Marca adquiridas todas las PENDIENTE de ese ISBN (salen del agrupado).
    /**
     * Marca como adquiridas todas las sugerencias pendientes de un ISBN y las saca del agrupado.
     * Solo GERENTE y ADMIN.
     *
     * @param isbn ISBN cuyas sugerencias pendientes se confirman
     * @param authentication identidad del gerente que confirma la adquisición
     * @return mapa con el ISBN y la cantidad de sugerencias confirmadas
     */
    @PostMapping("/confirmar-adquisicion")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> confirmAcquisition(
            @RequestParam String isbn,
            Authentication authentication) {
        Long revisorId = authentication == null ? null
                : suggestionService.resolveIdByEmailPublic(authentication.getName());
        int confirmadas = suggestionService.confirmAcquisition(isbn, revisorId);
        return ResponseEntity.ok(java.util.Map.of("isbn", isbn, "confirmadas", confirmadas));
    }

    // ── GET /api/v1/sugerencias-adquisicion/reporte-pdf ──
    /**
     * Descarga el reporte PDF de las sugerencias más pedidas agrupadas por ISBN.
     * Solo GERENTE y ADMIN.
     *
     * @return bytes del PDF con cabecera de descarga
     */
    @GetMapping("/reporte-pdf")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportPdf() {
        byte[] pdf = reportPdfService.generateReportSuggestionsMostRequested(
                suggestionService.getMostRequestedList());
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=reporte-sugerencias-mas-pedidas.pdf")
                .body(pdf);
    }
}
