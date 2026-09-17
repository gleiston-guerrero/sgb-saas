package com.uteq.backend.controller;

import com.uteq.backend.dto.FavoriteResponseDTO;
import com.uteq.backend.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;

// Favoritos del propio LECTOR; el usuarioId se resuelve del
// Authentication en FavoritoService.
@RestController
@RequestMapping("/api/v1/favoritos")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * Constructor con el servicio de favoritos.
     *
     * @param favoriteService servicio de favoritos del lector
     */
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // ── POST /api/v1/favoritos/{libroId} ──────────────────
    /**
     * Agrega un libro a los favoritos del propio LECTOR autenticado. Solo LECTOR.
     *
     * @param bookId id del libro a marcar como favorito
     * @param authentication identidad del LECTOR dueño de los favoritos
     * @return favorito creado con estado 201
     */
    @PostMapping("/{libroId}")
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<FavoriteResponseDTO> add(
            @PathVariable("libroId") Long bookId, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoriteService.add(bookId, authentication));
    }

    // ── DELETE /api/v1/favoritos/{libroId} ────────────────
    /**
     * Quita un libro de los favoritos del propio LECTOR autenticado. Solo LECTOR.
     *
     * @param bookId id del libro a quitar de favoritos
     * @param authentication identidad del LECTOR dueño de los favoritos
     * @return respuesta vacía con estado 204 si se quitó
     */
    @DeleteMapping("/{libroId}")
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<Void> remove(
            @PathVariable("libroId") Long bookId, Authentication authentication) {
        favoriteService.remove(bookId, authentication);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/v1/favoritos ──────────────────────────────
    // Roadmap original proponía /favoritos/usuario/{usuarioId}, pero el
    // usuarioId ya se resuelve del Authentication (ver FavoritoService) --
    // exponerlo también en el path permitiría a un LECTOR intentar leer
    // favoritos ajenos con solo cambiar el número en la URL, mismo tipo de
    // hallazgo IDOR que ya se corrigió en PrestamoService.validarAccesoUsuario.
    // Se deja sin path param a propósito: "mis favoritos", no "favoritos
    // de tal usuarioId".
    /**
     * Lista en forma paginada los favoritos del propio LECTOR autenticado. Solo LECTOR.
     *
     * @param authentication identidad del LECTOR cuyos favoritos se consultan
     * @param pageable paginación y orden solicitados
     * @return página con los favoritos del lector
     */
    @GetMapping
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<Page<FavoriteResponseDTO>> listOwns(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "agregado") Pageable pageable) {
        return ResponseEntity.ok(favoriteService.listOwnsPaginated(authentication, pageable));
    }
    /**
     * Lista todos los favoritos del propio LECTOR autenticado sin paginar. Solo LECTOR.
     *
     * @param authentication identidad del LECTOR cuyos favoritos se consultan
     * @return lista completa de favoritos del lector
     */
    @GetMapping("/todo")
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<List<FavoriteResponseDTO>> listOwnsAll(Authentication authentication) {
        return ResponseEntity.ok(favoriteService.listOwns(authentication));
    }
}
