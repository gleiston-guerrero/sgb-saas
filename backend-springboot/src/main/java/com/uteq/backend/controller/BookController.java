package com.uteq.backend.controller;

import com.uteq.backend.dto.BookRequestDTO;
import com.uteq.backend.dto.BookResponseDTO;
import com.uteq.backend.dto.BookSuggestionDTO;
import com.uteq.backend.dto.BookIsbnLookupDTO;
import com.uteq.backend.dto.CoverImageDTO;
import com.uteq.backend.service.BookService;
import com.uteq.backend.service.BookIsbnLookupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Catálogo de libros: búsqueda pública autenticada y gestión
 * (BIBLIOTECARIO/GERENTE/ADMIN), más portadas e ISBN externo.
 */
@RestController
@RequestMapping("/api/v1/libros")
@Validated
public class BookController {

    private final BookService bookService;
    private final BookIsbnLookupService bookIsbnLookupService;

    /**
     * Constructor con los servicios del catálogo.
     *
     * @param bookService servicio CRUD y búsquedas del catálogo
     * @param bookIsbnLookupService servicio de autocompletado por ISBN externo
     */
    public BookController(BookService bookService, BookIsbnLookupService bookIsbnLookupService) {
        this.bookService = bookService;
        this.bookIsbnLookupService = bookIsbnLookupService;
    }

    // ── GET /api/v1/libros?page=0&size=10 ────────────────
    // Filtros combinables: q (título/ISBN), estadoLibroId, categoriaId, autorId.
    // Si no se envía estadoLibroId, default = ACTIVO.
    /**
     * Lista el catálogo de libros con filtros combinables por texto, estado, categoría, autor y disponibilidad.
     * Sin estadoLibroId aplica el estado ACTIVO por defecto. Roles LECTOR, BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param q texto a buscar en título o ISBN, null para no filtrar
     * @param statusBookId id del estado del libro, null para el defecto ACTIVO
     * @param categoryId id de categoría, null para todas
     * @param authorId id de autor, null para todos
     * @param available true solo disponibles, false solo no disponibles, null todos
     * @param pageable paginación y orden solicitados
     * @return página de libros que cumplen los filtros
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<Page<BookResponseDTO>> list(
            @RequestParam(required = false) String q,
            @RequestParam(name = "estadoLibroId", required = false) Integer statusBookId,
            @RequestParam(name = "categoriaId", required = false) Integer categoryId,
            @RequestParam(name = "autorId", required = false) Long authorId,
            @RequestParam(name = "disponible", required = false) Boolean available,
            // sort con el nombre de PROPIEDAD JPA (title): BookService
            // traduce columna->propiedad (derivedSort, P5) antes de consultar.
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(bookService.listWithFilters(q, statusBookId, categoryId, authorId, available, pageable));
    }

    // ── GET /api/v1/libros/sugerencias?texto= ─────────────
    // Autocompletado de catálogo. Cualquier usuario autenticado puede buscar.
    /**
     * Devuelve sugerencias de autocompletado del catálogo para el texto dado.
     * Cualquier usuario autenticado puede usarlo.
     *
     * @param text texto parcial del título con entre 2 y 60 caracteres
     * @return lista de sugerencias de libros coincidentes
     */
    @GetMapping("/sugerencias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookSuggestionDTO>> suggestions(
            @RequestParam("texto") @Size(min = 2, max = 60, message = "El texto de búsqueda debe tener entre 2 y 60 caracteres") String text) {
        return ResponseEntity.ok(bookService.suggest(text));
    }

    // ── GET /api/v1/libros/pendientes ────────────────
    // Listado de libros en estados de gestión: DADO_DE_BAJA, PENDIENTE, EN_REPARACION, PERDIDO
    // Si no se envía estadoIds, usa los 4 por defecto.
    /**
     * Lista los libros en estados de gestión como DADO_DE_BAJA, PENDIENTE, EN_REPARACION o PERDIDO.
     * Sin estadoIds usa esos cuatro por defecto. Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param q texto a buscar en título o ISBN, null para no filtrar
     * @param yearPublication año de publicación a filtrar, null para todos
     * @param statusIds ids de estado a incluir, null para los cuatro de gestión
     * @param pageable paginación y orden solicitados
     * @return página de libros pendientes de revisión
     */
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<Page<BookResponseDTO>> pending(
            @RequestParam(required = false) String q,
            @RequestParam(name = "anioPublicacion", required = false) Integer yearPublication,
            @RequestParam(name = "estadoIds", required = false) List<Integer> statusIds,
            // sort con el nombre FISICO de columna (fecha_registro): listPending
            // usa searchByStatuses, query nativa incondicional (BookRepository).
            @PageableDefault(size = 10) @SortDefault(sort = "fecha_registro", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookService.listPending(q, yearPublication, statusIds, pageable));
    }

    // ── GET /api/v1/libros/lookup-isbn?isbn= ─────────────
    // Autocompletar desde Google Books. La ruta literal gana sobre /{id};
    // 404 con ProblemDetail si no hay resultado.
    /**
     * Busca los datos bibliográficos de un ISBN en Google Books para autocompletar el formulario.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param isbn ISBN de 10 a 13 dígitos a consultar en el servicio externo
     * @return datos del libro encontrado para precargar el formulario
     */
    @GetMapping("/lookup-isbn")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<BookIsbnLookupDTO> lookupIsbn(
            @RequestParam @Pattern(regexp = "^[0-9]{10,13}$", message = "ISBN debe tener 10 a 13 dígitos")
            @Size(min = 10, max = 13, message = "El ISBN debe tener entre 10 y 13 caracteres") String isbn) {
        return ResponseEntity.ok(bookIsbnLookupService.searchByIsbn(isbn));
    }

    // ── GET /api/v1/libros/lookup-isbn/portada?isbn= ─────
    // Proxy de la portada de Google Books: el backend descarga el
    // thumbnail y lo devuelve como binario (el navegador no debe llamar
    // a Google Books directo). Igual que /{id}/portada, 404 si no hay.
    /**
     * Actúa como proxy de la portada de Google Books y devuelve su binario para el ISBN dado.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. Responde 404 si no hay portada.
     *
     * @param isbn ISBN de 10 a 13 dígitos cuya portada externa se solicita
     * @return bytes de la imagen con su tipo de contenido original
     */
    @GetMapping("/lookup-isbn/portada")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<byte[]> lookupIsbnCover(
            @RequestParam @Pattern(regexp = "^[0-9]{10,13}$", message = "ISBN debe tener 10 a 13 dígitos")
            @Size(min = 10, max = 13, message = "El ISBN debe tener entre 10 y 13 caracteres") String isbn) {
        CoverImageDTO cover = bookIsbnLookupService.getCover(isbn);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .body(cover.bytes());
    }

    // ── GET /api/v1/libros/{id} ───────────────────────────
    /**
     * Obtiene el detalle de un libro del catálogo por su id.
     * Roles LECTOR, BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param id id del libro a consultar
     * @return detalle del libro solicitado
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<BookResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.searchById(id));
    }

    // ── POST /api/v1/libros ───────────────────────────────
    /**
     * Crea un libro nuevo en el catálogo. Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param dto datos del libro con título, ISBN, categoría, autor y existencias
     * @return libro creado con estado 201
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<BookResponseDTO> create(
            @Valid @RequestBody BookRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookService.create(dto));
    }

    // ── PUT /api/v1/libros/{id} ───────────────────────────
    /**
     * Actualiza los datos de un libro existente. Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param id id del libro a actualizar
     * @param dto datos nuevos del libro con título, ISBN, categoría, autor y existencias
     * @return libro actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<BookResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody BookRequestDTO dto) {
        return ResponseEntity.ok(bookService.update(id, dto));
    }

    // ── DELETE /api/v1/libros/{id} ────────────────────────
    /**
     * Elimina un libro del catálogo por su id. Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param id id del libro a eliminar
     * @return respuesta vacía con estado 204 si se eliminó
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── POST /api/v1/libros/{id}/portada ───────────────────
    // Subida multipart con campo "archivo". La validación de tipo/tamaño
    // vive en LibroService y responde 400 vía GlobalExceptionHandler.
    /**
     * Sube o reemplaza la imagen de portada de un libro mediante archivo multipart.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. El tipo y tamaño se validan en el servicio.
     *
     * @param id id del libro al que pertenece la portada
     * @param file archivo de imagen enviado en el campo archivo
     * @return libro actualizado con los datos de su portada
     */
    @PostMapping(value = "/{id}/portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<BookResponseDTO> uploadCover(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile file) {
        return ResponseEntity.ok(bookService.updateCover(id, file));
    }

    // ── GET /api/v1/libros/{id}/portada ────────────────────
    // Devuelve el binario con Content-Type dinámico según portada_tipo
    // (image/png|image/jpeg|image/webp). LECTURA para todos los roles
    // autenticados, igual que el resto del catálogo. 404 (no un
    // placeholder) si el libro no existe o no tiene portada -- eso es
    // decisión del frontend.
    /**
     * Descarga el binario de la portada guardada de un libro con su tipo de contenido.
     * Roles LECTOR, BIBLIOTECARIO, GERENTE y ADMIN. Responde 404 si no tiene portada.
     *
     * @param id id del libro cuya portada se solicita
     * @return bytes de la imagen con su tipo de contenido
     */
    @GetMapping("/{id}/portada")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<byte[]> getCover(@PathVariable Long id) {
        CoverImageDTO cover = bookService.getCover(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .body(cover.bytes());
    }
}
