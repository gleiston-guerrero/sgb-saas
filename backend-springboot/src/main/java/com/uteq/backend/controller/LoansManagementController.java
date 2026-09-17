package com.uteq.backend.controller;

import com.uteq.backend.dto.HistoryLoanDTO;
import com.uteq.backend.dto.ReservationActiveDTO;
import com.uteq.backend.dto.UserLoansManagementDTO;
import com.uteq.backend.dto.UserSuggestionDTO;
import com.uteq.backend.service.LoansManagementService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lecturas de la ventanilla de préstamos (módulo "Préstamos" del sidebar
 * del BIBLIOTECARIO): encontrar al usuario por correo y armar la pantalla
 * -- tarjeta de identificación, reserva vigente e historial reciente.
 *
 * La CREACIÓN del préstamo no vive acá: se reutiliza POST /api/v1/prestamos
 * (PrestamoController.crear), que ahora acepta reservacionId opcional para
 * convertir una reserva en préstamo. Mismo criterio de roles que el resto
 * de operaciones de ventanilla: BIBLIOTECARIO/GERENTE.
 */
@RestController
@RequestMapping("/api/v1/prestamos/gestion")
@Validated
public class LoansManagementController {

    private final LoansManagementService loansManagementService;

    /**
     * Constructor con el servicio de lecturas de la ventanilla de préstamos.
     *
     * @param loansManagementService servicio de búsqueda de usuarios y reservas para ventanilla
     */
    public LoansManagementController(LoansManagementService loansManagementService) {
        this.loansManagementService = loansManagementService;
    }

    // ── GET /api/v1/prestamos/gestion/buscar-usuario?correo= ──
    // Correo = usuarios.correo: es la identidad de login (UNIQUE), misma
    // columna que resuelve findByCorreo en el resto del sistema. 404 con
    // ProblemDetail si no hay coincidencia; el mensaje es el que muestra
    // la pantalla.
    /**
     * Busca al usuario por su correo exacto para armar la tarjeta de la ventanilla de préstamos.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. Responde 404 si no hay coincidencia.
     *
     * @param email correo completo del usuario a buscar
     * @return tarjeta del usuario con su identificación y estado
     */
    @GetMapping("/buscar-usuario")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<UserLoansManagementDTO> searchUser(
            @RequestParam("correo") @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
                     message = "Ingresa un correo electrónico válido")
            String email) {
        return ResponseEntity.ok(loansManagementService.searchByEmail(email));
    }

    // ── GET /api/v1/prestamos/gestion/sugerencias-usuarios?correo= ──
    // Autocompletado predictivo: retorna hasta 3 usuarios cuyo correo
    // contenga el texto ingresado (case-insensitive). El frontend lo usa
    // para el dropdown y el placeholder dinámico.
    /**
     * Sugiere hasta tres usuarios cuyo correo contenga el texto para el autocompletado de ventanilla.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param email texto parcial del correo para el autocompletado
     * @return lista de hasta tres usuarios coincidentes
     */
    @GetMapping("/sugerencias-usuarios")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<UserSuggestionDTO>> suggestionsUsers(
            @RequestParam("correo") String email) {
        return ResponseEntity.ok(loansManagementService.suggestionsUsers(email));
    }

    // ── GET /api/v1/prestamos/gestion/reserva-activa?usuarioId= ──
    // 404 cuando el usuario NO tiene reserva vigente -> el frontend cae al
    // Caso B (préstamo directo). No es un error para el usuario final.
    /**
     * Devuelve la reserva vigente de un usuario para convertirla en préstamo en ventanilla.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. Responde 404 si no tiene reserva vigente.
     *
     * @param userId id del usuario cuya reserva vigente se consulta
     * @return reserva activa con el libro reservado
     */
    @GetMapping("/reserva-activa")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<ReservationActiveDTO> reservationActive(@RequestParam("usuarioId") Long userId) {
        return ResponseEntity.ok(loansManagementService.reservationActive(userId));
    }

    // ── GET /api/v1/prestamos/gestion/historial?usuarioId= ────
    // Historial reciente (tope interno en el service) para la línea de
    // tiempo; lista vacía si el usuario no tiene préstamos.
    /**
     * Devuelve el historial reciente de préstamos de un usuario para la línea de tiempo de ventanilla.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. Lista vacía si no tiene préstamos.
     *
     * @param userId id del usuario cuyo historial se consulta
     * @return lista reciente de préstamos del usuario
     */
    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<HistoryLoanDTO>> history(@RequestParam("usuarioId") Long userId) {
        return ResponseEntity.ok(loansManagementService.history(userId));
    }
}
