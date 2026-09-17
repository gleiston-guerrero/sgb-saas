package com.uteq.backend.controller;

import com.uteq.backend.dto.CancellationFineRequestDTO;
import com.uteq.backend.dto.FineActionResponseDTO;
import com.uteq.backend.dto.FineDetailResponseDTO;
import com.uteq.backend.dto.FineResponseDTO;
import com.uteq.backend.dto.PaymentFineRequestDTO;
import com.uteq.backend.dto.SummaryFinancialFinesResponseDTO;
import com.uteq.backend.service.FineService;
import com.uteq.backend.service.NotificationService;
import com.uteq.backend.service.ReportPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/multas")
public class FineController {

    private final FineService fineService;
    private final NotificationService notificationService;
    private final ReportPdfService reportPdfService;

    /**
     * Constructor con los servicios de multas, notificaciones y reportes PDF.
     *
     * @param fineService servicio de consulta, pago y anulación de multas
     * @param notificationService servicio de avisos de comprobante de pago
     * @param reportPdfService servicio de generación de reportes en PDF
     */
    public FineController(FineService fineService, NotificationService notificationService, ReportPdfService reportPdfService) {
        this.fineService = fineService;
        this.notificationService = notificationService;
        this.reportPdfService = reportPdfService;
    }
    /**
     * Lista en forma paginada las multas de un usuario. Un LECTOR solo ve las suyas.
     * Roles LECTOR, BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param userId id del usuario cuyas multas se consultan
     * @param authentication identidad autenticada que pide la consulta
     * @param pageable paginación y orden solicitados
     * @return página de multas del usuario indicado
     */

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<Page<FineResponseDTO>> listByUser(
            @PathVariable("usuarioId") Long userId,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "dateGenerated") Pageable pageable) {
        return ResponseEntity.ok(
                fineService.listByUser(userId, authentication, pageable));
    }
    /**
     * Lista en forma paginada el detalle de multas de un usuario con su estado y saldos.
     * Un LECTOR solo ve el suyo. Roles LECTOR, BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param userId id del usuario cuyo detalle de multas se consulta
     * @param authentication identidad autenticada que pide la consulta
     * @param pageable paginación y orden solicitados
     * @return página con el detalle de multas del usuario indicado
     */

    @GetMapping("/usuario/{usuarioId}/detalle")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<Page<FineDetailResponseDTO>> listDetailByUser(
            @PathVariable("usuarioId") Long userId,
            Authentication authentication,
            // id como segundo criterio: statusFineId tiene cardinalidad 3 y
            // sin desempate la paginacion puede duplicar/saltar filas.
            @PageableDefault(size = 10, sort = {"statusFineId", "id"}) Pageable pageable) {
        return ResponseEntity.ok(
                fineService.listDetailByUser(userId, authentication, pageable));
    }
    /**
     * Registra el pago total o parcial de una multa y notifica el comprobante al usuario.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. Sin monto paga el total; con monto paga parcial.
     *
     * @param id id de la multa a pagar
     * @param body monto pagado, null para pagar el total pendiente
     * @return mapa con id de multa, estado resultante y saldo restante
     */

    @PostMapping("/{id}/pago")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<Map<String, Object>> pay(
            @PathVariable Long id,
            @RequestBody(required = false) PaymentFineRequestDTO body) {
        BigDecimal amountPaid = body != null ? body.amountPaid() : null;

        Map<String, Object> result;
        if (amountPaid != null) {
            result = fineService.payPartial(id, amountPaid);
        } else {
            var action = fineService.pay(id);
            result = Map.of(
                    "o_multa_id", action.fineId(),
                    "o_usuario_desbloqueado", action.userUnblocked(),
                    "o_estado", "PAGADA",
                    "o_saldo_restante", BigDecimal.ZERO);
        }

        Long userId = fineService.resolveUserIdFine(id);
        notificationService.notifyReceiptPayment(userId, id, amountPaid);

        return ResponseEntity.ok(result);
    }
    /**
     * Anula una multa registrando el motivo y el gerente que la anula. Solo GERENTE y ADMIN.
     *
     * @param id id de la multa a anular
     * @param dto motivo de la anulación
     * @param authentication identidad del gerente que autoriza la anulación
     * @return resultado de la anulación con el estado aplicado
     */
    @PostMapping("/{id}/anulacion")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<FineActionResponseDTO> annul(
            @PathVariable Long id,
            @Valid @RequestBody CancellationFineRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(fineService.annul(id, dto.reason(), authentication));
    }
    /**
     * Devuelve el resumen financiero de multas en el rango de fechas indicado. Solo GERENTE y ADMIN.
     *
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @return resumen financiero con totales cobrados y pendientes
     */

    @GetMapping("/reportes/resumen-financiero")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<SummaryFinancialFinesResponseDTO> reportSummaryFinancial(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        return ResponseEntity.ok(fineService.reportSummaryFinancial(from, until));
    }
    /**
     * Descarga el resumen financiero de multas como archivo PDF. Solo GERENTE y ADMIN.
     *
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @return bytes del PDF con cabecera de descarga
     */

    @GetMapping(value = "/reportes/resumen-financiero/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportSummaryFinancialPdf(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        SummaryFinancialFinesResponseDTO dto = fineService.reportSummaryFinancial(from, until);
        byte[] pdf = reportPdfService.generateReportSummaryFinancial(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-resumen-financiero.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
