package com.uteq.backend.controller;

import com.uteq.backend.dto.LoanReturnResponseDTO;
import com.uteq.backend.dto.BookMostLoanedDetailedResponseDTO;
import com.uteq.backend.dto.BookMostLoanedResponseDTO;
import com.uteq.backend.dto.LoanActiveResponseDTO;
import com.uteq.backend.dto.LoanRequestDTO;
import com.uteq.backend.dto.LoanResponseDTO;
import com.uteq.backend.dto.RenewalResponseDTO;
import com.uteq.backend.dto.ReportCategoriesDemandedResponseDTO;
import com.uteq.backend.dto.ReportInventoryResponseDTO;
import com.uteq.backend.dto.ReportDelinquencyResponseDTO;
import com.uteq.backend.dto.ReportUsageByPeriodResponseDTO;
import com.uteq.backend.dto.ReportOverduesResponseDTO;
import com.uteq.backend.service.LoanService;
import com.uteq.backend.service.ReportPdfService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prestamos")
public class LoanController {

    private final LoanService loanService;
    private final ReportPdfService reportPdfService;

    /**
     * Constructor con el servicio de préstamos y el generador de reportes PDF.
     *
     * @param loanService servicio de creación, renovación y reportes de préstamos
     * @param reportPdfService servicio de generación de reportes en PDF
     */
    public LoanController(LoanService loanService, ReportPdfService reportPdfService) {
        this.loanService = loanService;
        this.reportPdfService = reportPdfService;
    }

    // ── POST /api/v1/prestamos ────────────────────────────
    /**
     * Crea un préstamo de ventanilla, opcionalmente convirtiendo una reserva vigente.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param dto libro, usuario y reserva opcional del préstamo
     * @param authentication identidad del personal que registra el préstamo
     * @return préstamo creado con estado 201
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<LoanResponseDTO> create(
            @Valid @RequestBody LoanRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.create(dto, authentication));
    }

    // ── POST /api/v1/prestamos/{id}/devolucion ────────────
    /**
     * Registra la devolución simple de un préstamo sin inspección de daños.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param id id del préstamo a devolver
     * @return detalle de la devolución registrada
     */
    @PostMapping("/{id}/devolucion")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<LoanReturnResponseDTO> registerLoanReturn(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.registerLoanReturn(id));
    }

    // ── POST /api/v1/prestamos/{id}/renovacion ────────────
    // LECTOR solo su propio préstamo (verificado dentro de
    // PrestamoService.renovar()); BIBLIOTECARIO/GERENTE/ADMIN, cualquiera.
    /**
     * Renueva un préstamo extendiendo su fecha de vencimiento según el reglamento.
     * El LECTOR solo renueva los suyos; el personal renueva cualquiera.
     *
     * @param id id del préstamo a renovar
     * @param authentication identidad que solicita la renovación
     * @return renovación aplicada con la nueva fecha de vencimiento
     */
    @PostMapping("/{id}/renovacion")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<RenewalResponseDTO> renew(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(loanService.renew(id, authentication));
    }

    // ── GET /api/v1/prestamos/usuario/{usuarioId}?page=0&size=10 ──
    /**
     * Lista en forma paginada los préstamos de un usuario. Un LECTOR solo ve los suyos.
     * Roles LECTOR, BIBLIOTECARIO y GERENTE.
     *
     * @param userId id del usuario cuyos préstamos se consultan
     * @param authentication identidad autenticada que pide la consulta
     * @param pageable paginación y orden solicitados
     * @return página de préstamos del usuario indicado
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE')")
    public ResponseEntity<Page<LoanResponseDTO>> listByUser(
            @PathVariable("usuarioId") Long userId,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "dateLoan") Pageable pageable) {
        return ResponseEntity.ok(
                loanService.listByUser(userId, authentication, pageable));
    }

    // ── GET /api/v1/prestamos/usuario/{usuarioId}/activos ─
    /**
     * Lista los préstamos activos y vencidos de un usuario para la ventanilla de gestión.
     * Roles LECTOR, BIBLIOTECARIO y GERENTE. Un LECTOR solo ve los suyos.
     *
     * @param userId id del usuario cuyos préstamos activos se consultan
     * @param authentication identidad autenticada que pide la consulta
     * @return lista de préstamos activos del usuario indicado
     */
    @GetMapping("/usuario/{usuarioId}/activos")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE')")
    public ResponseEntity<List<LoanActiveResponseDTO>> listActivesByUser(
            @PathVariable("usuarioId") Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(
                loanService.listActivesByUser(userId, authentication));
    }

    // ── GET /api/v1/prestamos/reportes/libros-mas-prestados ──
    /**
     * Devuelve el ranking de libros más prestados en el rango de fechas indicado.
     * Solo GERENTE y ADMIN.
     *
     * @param limit tope de libros del ranking, null para el defecto del servicio
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @return lista de libros más prestados con su conteo
     */
    @GetMapping("/reportes/libros-mas-prestados")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<BookMostLoanedResponseDTO>> reportBooksMostLoaned(
            @RequestParam(name = "limite", required = false) Integer limit,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        return ResponseEntity.ok(
                loanService.reportBooksMostLoaned(limit, from, until));
    }

    // ── GET /api/v1/prestamos/reportes/libros-mas-prestados-detallado ──
    /**
     * Devuelve en forma paginada el detalle de libros más prestados con filtro por categoría.
     * Solo GERENTE y ADMIN.
     *
     * @param limit tope de libros del ranking, null para el defecto del servicio
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @param categoryId id de categoría a filtrar, null para todas
     * @param pageable paginación solicitada
     * @return página con el detalle de libros más prestados
     */
    @GetMapping("/reportes/libros-mas-prestados-detallado")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<BookMostLoanedDetailedResponseDTO>> reportBooksMostLoanedDetailed(
            @RequestParam(name = "limite", required = false) Integer limit,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until,
            @RequestParam(name = "categoriaId", required = false) Integer categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                loanService.reportBooksMostLoanedDetailedPaginated(limit, from, until, categoryId, pageable));
    }
    /**
     * Devuelve sin paginar todo el detalle de libros más prestados para exportación.
     * Solo GERENTE y ADMIN.
     *
     * @param limit tope de libros del ranking, null para el defecto del servicio
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @param categoryId id de categoría a filtrar, null para todas
     * @return lista completa del detalle de libros más prestados
     */

    @GetMapping("/reportes/libros-mas-prestados-detallado/todo")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<BookMostLoanedDetailedResponseDTO>> reportBooksMostLoanedDetailedAll(
            @RequestParam(name = "limite", required = false) Integer limit,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until,
            @RequestParam(name = "categoriaId", required = false) Integer categoryId) {
        return ResponseEntity.ok(loanService.reportBooksMostLoanedDetailed(limit, from, until, categoryId));
    }

    // ── GET /api/v1/prestamos/reportes/morosidad ──────────
    /**
     * Devuelve en forma paginada el reporte de usuarios con morosidad vigente. Solo GERENTE y ADMIN.
     *
     * @param limit tope de filas del reporte, null para el defecto del servicio
     * @param pageable paginación solicitada
     * @return página de usuarios morosos con sus atrasos
     */
    @GetMapping("/reportes/morosidad")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<ReportDelinquencyResponseDTO>> reportDelinquency(
            @RequestParam(name = "limite", required = false) Integer limit,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.reportDelinquencyPaginated(limit, pageable));
    }
    /**
     * Devuelve sin paginar todo el reporte de usuarios con morosidad vigente. Solo GERENTE y ADMIN.
     *
     * @param limit tope de filas del reporte, null para el defecto del servicio
     * @return lista completa de usuarios morosos con sus atrasos
     */

    @GetMapping("/reportes/morosidad/todo")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<ReportDelinquencyResponseDTO>> reportDelinquencyAll(
            @RequestParam(name = "limite", required = false) Integer limit) {
        return ResponseEntity.ok(loanService.reportDelinquency(limit));
    }

    // ── GET /api/v1/prestamos/reportes/uso?granularidad=dia|semana|mes ──
    /**
     * Devuelve en forma paginada el uso de préstamos agrupado por día, semana o mes.
     * Solo GERENTE y ADMIN.
     *
     * @param granularidad agrupación temporal con valores dia, semana o mes
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @param pageable paginación solicitada
     * @return página de uso de préstamos por período
     */
    @GetMapping("/reportes/uso")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<ReportUsageByPeriodResponseDTO>> reportUsageByPeriod(
            @RequestParam(required = false, defaultValue = "dia") String granularidad,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.reportUsageByPeriodPaginated(granularidad, from, until, pageable));
    }
    /**
     * Devuelve sin paginar todo el uso de préstamos agrupado por día, semana o mes.
     * Solo GERENTE y ADMIN.
     *
     * @param granularidad agrupación temporal con valores dia, semana o mes
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @return lista completa de uso de préstamos por período
     */

    @GetMapping("/reportes/uso/todo")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<ReportUsageByPeriodResponseDTO>> reportUsageByPeriodAll(
            @RequestParam(required = false, defaultValue = "dia") String granularidad,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        return ResponseEntity.ok(loanService.reportUsageByPeriod(granularidad, from, until));
    }

    // ── GET /api/v1/prestamos/reportes/morosidad/pdf ──────
    /**
     * Descarga el reporte de morosidad como archivo PDF. Solo GERENTE y ADMIN.
     *
     * @param limit tope de filas del reporte, null para el defecto del servicio
     * @return bytes del PDF con cabecera de descarga
     */
    @GetMapping(value = "/reportes/morosidad/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportDelinquencyPdf(
            @RequestParam(name = "limite", required = false) Integer limit) {
        List<ReportDelinquencyResponseDTO> report = loanService.reportDelinquency(limit);
        byte[] pdf = reportPdfService.generateReportDelinquency(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-morosidad.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── GET /api/v1/prestamos/reportes/libros-mas-prestados/pdf ──
    /**
     * Descarga el ranking detallado de libros más prestados como archivo PDF. Solo GERENTE y ADMIN.
     *
     * @param limit tope de libros del ranking, null para el defecto del servicio
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @param categoryId id de categoría a filtrar, null para todas
     * @return bytes del PDF con cabecera de descarga
     */
    @GetMapping(value = "/reportes/libros-mas-prestados/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportBooksMostLoanedPdf(
            @RequestParam(name = "limite", required = false) Integer limit,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until,
            @RequestParam(name = "categoriaId", required = false) Integer categoryId) {
        List<BookMostLoanedDetailedResponseDTO> report =
                loanService.reportBooksMostLoanedDetailed(limit, from, until, categoryId);
        byte[] pdf = reportPdfService.generateReportBooksMostLoaned(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-libros-prestados.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── GET /api/v1/prestamos/reportes/inventario/pdf ─────
    /**
     * Descarga el reporte de inventario como archivo PDF con los filtros gerenciales.
     * Solo GERENTE y ADMIN.
     *
     * @param categoryId id de categoría a filtrar, null para todas
     * @param statusStock estado de stock a filtrar, null para todos
     * @param busqueda texto a buscar en título o ISBN, null sin filtro
     * @return bytes del PDF con cabecera de descarga
     */
    @GetMapping(value = "/reportes/inventario/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportInventoryPdf(
            @RequestParam(name = "categoriaId", required = false) Integer categoryId,
            @RequestParam(name = "estadoStock", required = false) String statusStock,
            @RequestParam(required = false) String busqueda) {
        List<ReportInventoryResponseDTO> report =
                loanService.reportInventory(categoryId, statusStock, busqueda);
        byte[] pdf = reportPdfService.generateReportInventory(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-inventario.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── GET /api/v1/prestamos/reportes/vencidos/pdf ───────
    /**
     * Descarga el reporte de préstamos vencidos como archivo PDF. Solo GERENTE y ADMIN.
     *
     * @param daysAtrasoMin mínimo de días de atraso a incluir, null sin mínimo
     * @param busqueda texto a buscar en título o usuario, null sin filtro
     * @return bytes del PDF con cabecera de descarga
     */
    @GetMapping(value = "/reportes/vencidos/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportOverduesPdf(
            @RequestParam(name = "diasAtrasoMin", required = false) Integer daysAtrasoMin,
            @RequestParam(required = false) String busqueda) {
        List<ReportOverduesResponseDTO> report =
                loanService.reportLoansOverdues(daysAtrasoMin, busqueda);
        byte[] pdf = reportPdfService.generateReportOverdues(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-vencidos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── GET /api/v1/prestamos/reportes/categorias-demandadas/pdf ──
    /**
     * Descarga el reporte de categorías más demandadas como archivo PDF. Solo GERENTE y ADMIN.
     *
     * @param limit tope de categorías del ranking, null para el defecto del servicio
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @return bytes del PDF con cabecera de descarga
     */
    @GetMapping(value = "/reportes/categorias-demandadas/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportCategoriesDemandedPdf(
            @RequestParam(name = "limite", required = false) Integer limit,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        List<ReportCategoriesDemandedResponseDTO> report =
                loanService.reportCategoriesDemanded(limit, from, until);
        byte[] pdf = reportPdfService.generateReportCategoriesDemanded(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-categorias.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── GET /api/v1/prestamos/reportes/uso/pdf ───────────
    /**
     * Descarga el reporte de uso por período como archivo PDF. Solo GERENTE y ADMIN.
     *
     * @param granularidad agrupación temporal con valores dia, semana o mes
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @return bytes del PDF con cabecera de descarga
     */
    @GetMapping(value = "/reportes/uso/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<byte[]> reportUsagePdf(
            @RequestParam(required = false, defaultValue = "dia") String granularidad,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        List<ReportUsageByPeriodResponseDTO> report = loanService.reportUsageByPeriod(granularidad, from, until);
        byte[] pdf = reportPdfService.generateReportUsageByPeriod(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-uso-periodo.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── GET /api/v1/prestamos/reportes/inventario ─────────
    // Paginacion real + 8 filtros gerenciales (categoria/editorial/año/stock/ubicacion/proveedor/estado/idioma)
    /**
     * Devuelve en forma paginada el inventario con filtros de categoría, stock, editorial y ubicación.
     * Solo GERENTE y ADMIN.
     *
     * @param categoryId id de categoría a filtrar, null para todas
     * @param statusStock estado de stock a filtrar, null para todos
     * @param busqueda texto a buscar en título o ISBN, null sin filtro
     * @param publisherId id de editorial a filtrar, null para todas
     * @param supplierId id de proveedor a filtrar, null para todos
     * @param statusBookId id de estado del libro a filtrar, null para todos
     * @param languageId id de idioma a filtrar, null para todos
     * @param yearFrom año de publicación inicial, null sin límite
     * @param yearUntil año de publicación final, null sin límite
     * @param stockTotalMin stock total mínimo, null sin mínimo
     * @param stockTotalMax stock total máximo, null sin máximo
     * @param stockDispMin stock disponible mínimo, null sin mínimo
     * @param stockDispMax stock disponible máximo, null sin máximo
     * @param location ubicación a filtrar, null para todas
     * @param pageable paginación solicitada
     * @return página del inventario con los filtros aplicados
     */
    @GetMapping("/reportes/inventario")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<ReportInventoryResponseDTO>> reportInventory(
            @RequestParam(name = "categoriaId", required = false) Integer categoryId,
            @RequestParam(name = "estadoStock", required = false) String statusStock,
            @RequestParam(required = false) String busqueda,
            @RequestParam(name = "editorialId", required = false) Integer publisherId,
            @RequestParam(name = "proveedorId", required = false) Integer supplierId,
            @RequestParam(name = "estadoLibroId", required = false) Integer statusBookId,
            @RequestParam(name = "idiomaId", required = false) Integer languageId,
            @RequestParam(name = "anioDesde", required = false) Short yearFrom,
            @RequestParam(name = "anioHasta", required = false) Short yearUntil,
            @RequestParam(required = false) Short stockTotalMin,
            @RequestParam(required = false) Short stockTotalMax,
            @RequestParam(required = false) Short stockDispMin,
            @RequestParam(required = false) Short stockDispMax,
            @RequestParam(name = "ubicacion", required = false) String location,
            // Sin sort por defecto: reportInventoryPaginated solo lee
            // pageSize/offset hacia fn_reporte_inventario (el orden lo fija
            // la funcion); un sort aqui viajaria en el PageImpl sin
            // aplicarse al SQL.
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.reportInventoryPaginated(
                categoryId, statusStock, busqueda, publisherId, supplierId, statusBookId, languageId,
                yearFrom, yearUntil, stockTotalMin, stockTotalMax, stockDispMin, stockDispMax, location, pageable));
    }
    /**
     * Devuelve sin paginar todo el inventario con filtros de categoría, stock, editorial y ubicación.
     * Solo GERENTE y ADMIN.
     *
     * @param categoryId id de categoría a filtrar, null para todas
     * @param statusStock estado de stock a filtrar, null para todos
     * @param busqueda texto a buscar en título o ISBN, null sin filtro
     * @param publisherId id de editorial a filtrar, null para todas
     * @param supplierId id de proveedor a filtrar, null para todos
     * @param statusBookId id de estado del libro a filtrar, null para todos
     * @param languageId id de idioma a filtrar, null para todos
     * @param yearFrom año de publicación inicial, null sin límite
     * @param yearUntil año de publicación final, null sin límite
     * @param stockTotalMin stock total mínimo, null sin mínimo
     * @param stockTotalMax stock total máximo, null sin máximo
     * @param stockDispMin stock disponible mínimo, null sin mínimo
     * @param stockDispMax stock disponible máximo, null sin máximo
     * @param location ubicación a filtrar, null para todas
     * @return lista completa del inventario con los filtros aplicados
     */

    @GetMapping("/reportes/inventario/todo")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<ReportInventoryResponseDTO>> reportInventoryAll(
            @RequestParam(name = "categoriaId", required = false) Integer categoryId,
            @RequestParam(name = "estadoStock", required = false) String statusStock,
            @RequestParam(required = false) String busqueda,
            @RequestParam(name = "editorialId", required = false) Integer publisherId,
            @RequestParam(name = "proveedorId", required = false) Integer supplierId,
            @RequestParam(name = "estadoLibroId", required = false) Integer statusBookId,
            @RequestParam(name = "idiomaId", required = false) Integer languageId,
            @RequestParam(name = "anioDesde", required = false) Short yearFrom,
            @RequestParam(name = "anioHasta", required = false) Short yearUntil,
            @RequestParam(required = false) Short stockTotalMin,
            @RequestParam(required = false) Short stockTotalMax,
            @RequestParam(required = false) Short stockDispMin,
            @RequestParam(required = false) Short stockDispMax,
            @RequestParam(name = "ubicacion", required = false) String location) {
        return ResponseEntity.ok(loanService.reportInventory(
                categoryId, statusStock, busqueda, publisherId, supplierId, statusBookId, languageId,
                yearFrom, yearUntil, stockTotalMin, stockTotalMax, stockDispMin, stockDispMax, location));
    }

    // ── GET /api/v1/prestamos/reportes/vencidos ───────────
    /**
     * Devuelve en forma paginada los préstamos vencidos con sus días de atraso.
     * Solo GERENTE y ADMIN.
     *
     * @param daysAtrasoMin mínimo de días de atraso a incluir, null sin mínimo
     * @param busqueda texto a buscar en título o usuario, null sin filtro
     * @param pageable paginación solicitada
     * @return página de préstamos vencidos
     */
    @GetMapping("/reportes/vencidos")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<ReportOverduesResponseDTO>> reportLoansOverdues(
            @RequestParam(name = "diasAtrasoMin", required = false) Integer daysAtrasoMin,
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.reportLoansOverduesPaginated(daysAtrasoMin, busqueda, pageable));
    }
    /**
     * Devuelve sin paginar todos los préstamos vencidos con sus días de atraso.
     * Solo GERENTE y ADMIN.
     *
     * @param daysAtrasoMin mínimo de días de atraso a incluir, null sin mínimo
     * @param busqueda texto a buscar en título o usuario, null sin filtro
     * @return lista completa de préstamos vencidos
     */

    @GetMapping("/reportes/vencidos/todo")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<ReportOverduesResponseDTO>> reportLoansOverduesAll(
            @RequestParam(name = "diasAtrasoMin", required = false) Integer daysAtrasoMin,
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(loanService.reportLoansOverdues(daysAtrasoMin, busqueda));
    }

    // ── GET /api/v1/prestamos/reportes/categorias-demandadas ──
    /**
     * Devuelve en forma paginada el ranking de categorías más demandadas en préstamo.
     * Solo GERENTE y ADMIN.
     *
     * @param limit tope de categorías del ranking, null para el defecto del servicio
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @param pageable paginación solicitada
     * @return página de categorías más demandadas con su conteo
     */
    @GetMapping("/reportes/categorias-demandadas")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<ReportCategoriesDemandedResponseDTO>> reportCategoriesDemanded(
            @RequestParam(name = "limite", required = false) Integer limit,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.reportCategoriesDemandedPaginated(limit, from, until, pageable));
    }
    /**
     * Devuelve sin paginar todo el ranking de categorías más demandadas en préstamo.
     * Solo GERENTE y ADMIN.
     *
     * @param limit tope de categorías del ranking, null para el defecto del servicio
     * @param from fecha inicial del rango, null sin límite inferior
     * @param until fecha final del rango, null sin límite superior
     * @return lista completa de categorías más demandadas con su conteo
     */

    @GetMapping("/reportes/categorias-demandadas/todo")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<ReportCategoriesDemandedResponseDTO>> reportCategoriesDemandedAll(
            @RequestParam(name = "limite", required = false) Integer limit,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        return ResponseEntity.ok(loanService.reportCategoriesDemanded(limit, from, until));
    }
}
