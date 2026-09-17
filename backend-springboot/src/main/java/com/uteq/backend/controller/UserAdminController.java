package com.uteq.backend.controller;

import com.uteq.backend.dto.ChangeStatusUserRequestDTO;
import com.uteq.backend.dto.ChangeRoleRequestDTO;
import com.uteq.backend.dto.CreateUserAdminRequestDTO;
import com.uteq.backend.dto.UserListingResponseDTO;
import com.uteq.backend.dto.UserResponseDTO;
import com.uteq.backend.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Panel de administración de usuarios. GERENTE opera sobre sus creados;
 * solo ADMIN crea GERENTE/ADMIN, ve todo y elimina (soft).
 */
@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class UserAdminController {

    private final UserAdminService userAdminService;

    /**
     * Constructor con el servicio de administración de usuarios.
     *
     * @param userAdminService servicio de listado, creación, roles y estados de usuarios
     */
    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    // ── GET /api/v1/admin/usuarios?filtro=&page=&size=&mios= ────
    // F8-gerente: ?mios=true filtra por creado_por propio (el service además
    // fuerza ese filtro para GERENTE aunque no mande el flag).
    /**
     * Lista en forma paginada los usuarios con filtro por texto y alcance propio para GERENTE.
     * Roles ADMIN y GERENTE. El GERENTE solo ve los usuarios que él creó.
     *
     * @param filter texto a buscar en nombre o correo, null para no filtrar
     * @param mios true para ver solo los creados por el solicitante
     * @param pageable paginación y orden solicitados
     * @param authentication identidad del administrador que pide el listado
     * @return página de usuarios según el alcance permitido
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Page<UserListingResponseDTO>> list(
            @RequestParam(name = "filtro", required = false) String filter,
            @RequestParam(required = false, defaultValue = "false") boolean mios,
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(userAdminService.list(filter, pageable, authentication, mios));
    }

    // ── PATCH /api/v1/admin/usuarios/{id}/rol ─────────────
    // F8-gerente: GERENTE limitado en service a sus creados + LECTOR/BIBLIOTECARIO.
    /**
     * Cambia el rol de un usuario. El GERENTE solo actúa sobre sus creados y a LECTOR o BIBLIOTECARIO.
     * Roles ADMIN y GERENTE.
     *
     * @param id id del usuario cuyo rol se cambia
     * @param dto rol nuevo solicitado
     * @param authentication identidad del administrador que autoriza el cambio
     * @return respuesta vacía con estado 204 si se aplicó
     */
    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequestDTO dto,
            Authentication authentication) {
        userAdminService.changeRole(id, dto.freshRole(), authentication);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH /api/v1/admin/usuarios/{id}/estado ──────────
    // F8-gerente: GERENTE limitado en service a sus creados + ACTIVO/INACTIVO.
    /**
     * Cambia el estado de un usuario registrando el motivo. El GERENTE solo usa ACTIVO o INACTIVO.
     * Roles ADMIN y GERENTE.
     *
     * @param id id del usuario cuyo estado se cambia
     * @param dto estado nuevo y motivo del cambio
     * @param authentication identidad del administrador que autoriza el cambio
     * @return respuesta vacía con estado 204 si se aplicó
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusUserRequestDTO dto,
            Authentication authentication) {
        userAdminService.changeStatus(id, dto.freshStatus(), dto.reason(), authentication);
        return ResponseEntity.noContent().build();
    }

    // ── POST /api/v1/admin/usuarios ──────────
    // F8-gerente: GERENTE crea solo LECTOR/BIBLIOTECARIO (service lo verifica).
    /**
     * Crea un usuario desde el panel. El GERENTE solo crea LECTOR o BIBLIOTECARIO.
     * Roles ADMIN y GERENTE.
     *
     * @param dto datos del usuario nuevo con rol y datos personales
     * @param authentication identidad del administrador que crea al usuario
     * @return usuario creado con estado 201
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody CreateUserAdminRequestDTO dto, Authentication authentication) {
        UserResponseDTO created = userAdminService.createUser(dto, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── DELETE /api/v1/admin/usuarios/{id} soft INACTIVO ──────────
    /**
     * Desactiva un usuario con borrado lógico a estado INACTIVO registrando el motivo. Solo ADMIN.
     *
     * @param id id del usuario a desactivar
     * @param reason motivo de la baja, null si no se indica
     * @param authentication identidad del administrador que ejecuta la baja
     * @return respuesta vacía con estado 204 si se aplicó
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(name = "motivo", required = false) String reason, Authentication authentication) {
        userAdminService.deleteUser(id, reason, authentication);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/v1/admin/usuarios/{id}/historial-motivos ──────────
    // V50/OBS-28: historial de motivos de cambio de estado/eliminación,
    // más reciente primero.
    /**
     * Devuelve el historial de motivos de cambios de estado y bajas de un usuario, el más reciente primero.
     * Roles ADMIN y GERENTE.
     *
     * @param id id del usuario cuyo historial de motivos se consulta
     * @return lista de cambios de estado con su motivo y fecha
     */
    @GetMapping("/{id}/historial-motivos")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<java.util.List<com.uteq.backend.dto.UserReasonChangeResponseDTO>> historyReasons(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.historyReasons(id));
    }
}
