package com.uteq.backend.controller;

import com.uteq.backend.dto.ChangeStatusReservationRequestDTO;
import com.uteq.backend.dto.ReservationTodayResponseDTO;
import com.uteq.backend.dto.ReservationRequestDTO;
import com.uteq.backend.dto.ReservationResponseDTO;
import com.uteq.backend.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservaciones")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Constructor con el servicio de reservaciones.
     *
     * @param reservationService servicio de creación, consulta y cambio de estado de reservas
     */
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // ── POST /api/v1/reservaciones ────────────────────────
    /**
     * Crea una reservación de un libro para el usuario autenticado o el indicado por el personal.
     * Roles LECTOR, BIBLIOTECARIO y GERENTE.
     *
     * @param dto libro, usuario y fecha de retiro de la reserva
     * @param authentication identidad que solicita la reserva
     * @return reservación creada con estado 201
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE')")
    public ResponseEntity<ReservationResponseDTO> create(
            @Valid @RequestBody ReservationRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.create(dto, authentication));
    }

    // ── GET /api/v1/reservaciones/hoy ──────────────────────
    // Dashboard del bibliotecario: reservaciones que vencen hoy, sin
    // paginar (volumen bajo por diseño -- es "las de hoy", no el histórico).
    /**
     * Lista las reservaciones que vencen hoy para el tablero del bibliotecario, sin paginar.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @return lista de reservaciones con vencimiento de hoy
     */
    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<ReservationTodayResponseDTO>> reservationsToday() {
        return ResponseEntity.ok(reservationService.searchReservationsToday());
    }
    /**
     * Lista las reservaciones próximas a vencer para la gestión del bibliotecario, sin paginar.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @return lista de reservaciones próximas con su fecha de retiro
     */
    @GetMapping("/proximas")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<ReservationTodayResponseDTO>> reservationsNexts() {
        return ResponseEntity.ok(reservationService.searchReservationsNexts());
    }

    // ── PATCH /api/v1/reservaciones/{id}/estado ────────────
    // El staff acepta (PENDIENTE -> LISTA_PARA_RETIRO) o rechaza
    // (PENDIENTE -> CANCELADA) la reservación de un lector. Es la acción
    // manual que faltaba del RF-10: hasta ahora el LECTOR podía crear y el
    // sistema expirar, pero nadie podía marcar "listo para retirar".
    /**
     * Cambia el estado de una reservación como aceptar a lista para retiro, rechazar o cancelar.
     * Roles LECTOR, BIBLIOTECARIO, GERENTE y ADMIN según la transición permitida.
     *
     * @param id id de la reservación a actualizar
     * @param dto estado nuevo solicitado para la reserva
     * @param authentication identidad que autoriza el cambio de estado
     * @return reservación actualizada con su estado nuevo
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<ReservationResponseDTO> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusReservationRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(reservationService.changeStatus(id, dto, authentication));
    }

    // ── GET /api/v1/reservaciones/usuario/{usuarioId} ─────
    /**
     * Lista en forma paginada las reservaciones de un usuario. Un LECTOR solo ve las suyas.
     * Roles LECTOR, BIBLIOTECARIO y GERENTE.
     *
     * @param userId id del usuario cuyas reservaciones se consultan
     * @param authentication identidad autenticada que pide la consulta
     * @param pageable paginación y orden solicitados
     * @return página de reservaciones del usuario indicado
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('LECTOR','BIBLIOTECARIO','GERENTE')")
    public ResponseEntity<Page<ReservationResponseDTO>> listByUser(
            @PathVariable("usuarioId") Long userId,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "dateReservation") Pageable pageable) {
        return ResponseEntity.ok(
                reservationService.listByUser(userId, authentication, pageable));
    }
}