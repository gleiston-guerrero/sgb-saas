package com.uteq.backend.service;

import com.uteq.backend.dto.UserListingResponseDTO;
import com.uteq.backend.entity.StatusUser;
import com.uteq.backend.entity.Role;
import com.uteq.backend.entity.User;
import com.uteq.backend.entity.UserReasonChange;
import com.uteq.backend.repository.StatusFineRepository;
import com.uteq.backend.repository.StatusUserRepository;
import com.uteq.backend.repository.FineRepository;
import com.uteq.backend.repository.RoleRepository;
import com.uteq.backend.repository.UserReasonChangeRepository;
import com.uteq.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository userRepo;
    @Mock RoleRepository roleRepo;
    @Mock StatusUserRepository statusUserRepo;
    @Mock FineRepository fineRepo;
    @Mock StatusFineRepository statusFineRepo;
    @Mock UserReasonChangeRepository userReasonChangeRepo;
    @Mock Authentication authentication;

    @InjectMocks UserAdminService service;

    private StatusUser status(String name) {
        return status(name, 1);
    }

    private StatusUser status(String name, int id) {
        StatusUser e = new StatusUser();
        e.setId(id);
        e.setName(name);
        return e;
    }

    private Role role(String name) {
        Role r = new Role();
        r.setId(1);
        r.setName(name);
        return r;
    }

    private User user(Long id, String email, String roleName, String statusName) {
        Instant ahora = Instant.now();
        return User.builder()
                .id(id)
                .name("Nombre")
                .lastName("Apellido")
                .email(email)
                .passwordHash("hash")
                .status(status(statusName))
                .emailVerified(true)
                .roles(Set.of(role(roleName)))
                .dateRegistration(ahora)
                .updated(ahora)
                .build();
    }

    // ── listar ──────────────────────────────────────────────

    @Test
    void list_retornaUsersMapeadosYMarkFinesPendientesSegunStatus() {
        User blocked = user(1L, "bloqueado@correo.com", "LECTOR", "BLOQUEADO_POR_MULTA");
        User active = user(2L, "activo@correo.com", "LECTOR", "ACTIVO");
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(blocked, active), pageable, 2);

        given(userRepo.searchWithFilters("", null, pageable))
                .willReturn(page);

        com.uteq.backend.entity.StatusFine statusPending = new com.uteq.backend.entity.StatusFine();
        statusPending.setId(1);
        statusPending.setName("PENDIENTE");
        given(statusFineRepo.findByName("PENDIENTE")).willReturn(Optional.of(statusPending));
        given(fineRepo.findUserIdsWithFinesPendientes(List.of(1L, 2L), 1))
                .willReturn(List.of(1L));

        Page<UserListingResponseDTO> result = service.list(null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).finesPendientes()).isTrue();
        assertThat(result.getContent().get(1).finesPendientes()).isFalse();
        assertThat(result.getContent().get(0).roles()).containsExactly("LECTOR");
    }

    // ── cambiarRol ──────────────────────────────────────────

    @Test
    void changeRole_withDataValids_actualizaRoles() {
        User userObjetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        User admin = user(9L, "admin@correo.com", "ADMIN", "ACTIVO");

        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(userObjetivo));
        given(roleRepo.findByName("BIBLIOTECARIO")).willReturn(Optional.of(role("BIBLIOTECARIO")));
        given(authentication.getName()).willReturn("admin@correo.com");
        given(userRepo.findByEmail("admin@correo.com")).willReturn(Optional.of(admin));

        service.changeRole(5L, "BIBLIOTECARIO", authentication);

        assertThat(userObjetivo.getRoles()).extracting(Role::getName).containsExactly("BIBLIOTECARIO");
        verify(userRepo, times(1)).save(userObjetivo);
    }

    @Test
    void changeRole_withUserInexistente_lanzaEntityNotFound() {
        given(userRepo.findByIdWithStatusAndRoles(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(404L, "ADMIN", authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void changeRole_withRoleInexistenteCatalogo_lanzaIllegalArgument() {
        User userObjetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(userObjetivo));
        given(roleRepo.findByName("SUPERVISOR")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(5L, "SUPERVISOR", authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }

    // ── cambiarEstado ───────────────────────────────────────

    @Test
    void changeStatus_withDataValids_actualizaStatusYRegistraReason() {
        User userObjetivo = User.builder()
                .id(5L).name("Nombre").lastName("Apellido").email("lector@correo.com")
                .passwordHash("hash").status(status("ACTIVO", 3)).emailVerified(true)
                .roles(Set.of(role("LECTOR"))).dateRegistration(Instant.now()).updated(Instant.now())
                .build();
        User admin = user(9L, "admin@correo.com", "ADMIN", "ACTIVO");

        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(userObjetivo));
        given(statusUserRepo.findByName("INACTIVO")).willReturn(Optional.of(status("INACTIVO", 2)));
        given(authentication.getName()).willReturn("admin@correo.com");
        given(userRepo.findByEmail("admin@correo.com")).willReturn(Optional.of(admin));

        service.changeStatus(5L, "INACTIVO", "Solicitud de baja voluntaria", authentication);

        assertThat(userObjetivo.getStatus().getName()).isEqualTo("INACTIVO");

        // V50/OBS-28: el motivo ya no se pierde -- queda en usuario_motivos_cambio.
        ArgumentCaptor<UserReasonChange> captor = ArgumentCaptor.forClass(UserReasonChange.class);
        verify(userReasonChangeRepo).save(captor.capture());
        UserReasonChange row = captor.getValue();
        assertThat(row.getUserId()).isEqualTo(5L);
        assertThat(row.getTypeChange()).isEqualTo("CAMBIO_ESTADO");
        assertThat(row.getStatusAnterior()).isEqualTo(3);
        assertThat(row.getStatusFresh()).isEqualTo(2);
        assertThat(row.getReason()).isEqualTo("Solicitud de baja voluntaria");
        assertThat(row.getExecutedBy()).isEqualTo(9L);
    }

    @Test
    void changeStatus_withStatusInexistenteCatalogo_lanzaIllegalArgument() {
        User userObjetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(userObjetivo));
        given(statusUserRepo.findByName("SUSPENDIDO")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(5L, "SUSPENDIDO", "motivo", authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUSPENDIDO");
    }

    @Test
    void changeStatus_withUserInexistente_lanzaEntityNotFoundYNotConsultaStatuses() {
        given(userRepo.findByIdWithStatusAndRoles(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(404L, "INACTIVO", "motivo", authentication))
                .isInstanceOf(EntityNotFoundException.class);

        verify(statusUserRepo, times(0)).findByName(any());
    }

    // ── eliminarUsuario ─────────────────────────────────────

    @Test
    void deleteUser_withReason_markInactivoYRegistraReason() {
        User userObjetivo = User.builder()
                .id(6L).name("Nombre").lastName("Apellido").email("lector2@correo.com")
                .passwordHash("hash").status(status("ACTIVO", 3)).emailVerified(true)
                .roles(Set.of(role("LECTOR"))).dateRegistration(Instant.now()).updated(Instant.now())
                .build();
        User admin = user(9L, "admin@correo.com", "ADMIN", "ACTIVO");

        given(userRepo.findByIdWithStatusAndRoles(6L)).willReturn(Optional.of(userObjetivo));
        given(statusUserRepo.findByName("INACTIVO")).willReturn(Optional.of(status("INACTIVO", 2)));
        given(authentication.getName()).willReturn("admin@correo.com");
        given(userRepo.findByEmail("admin@correo.com")).willReturn(Optional.of(admin));

        service.deleteUser(6L, "Solicitud propia de baja", authentication);

        assertThat(userObjetivo.getStatus().getName()).isEqualTo("INACTIVO");

        ArgumentCaptor<UserReasonChange> captor = ArgumentCaptor.forClass(UserReasonChange.class);
        verify(userReasonChangeRepo).save(captor.capture());
        UserReasonChange row = captor.getValue();
        assertThat(row.getUserId()).isEqualTo(6L);
        assertThat(row.getTypeChange()).isEqualTo("ELIMINACION");
        assertThat(row.getStatusAnterior()).isEqualTo(3);
        assertThat(row.getStatusFresh()).isEqualTo(2);
        assertThat(row.getReason()).isEqualTo("Solicitud propia de baja");
        assertThat(row.getExecutedBy()).isEqualTo(9L);
    }

    // El controller permite DELETE sin ?motivo (query param opcional) --
    // se persiste tal cual llega, sin inventar un texto de reemplazo.
    @Test
    void deleteUser_withoutReason_persisteReasonNulo() {
        User userObjetivo = user(7L, "lector3@correo.com", "LECTOR", "ACTIVO");
        User admin = user(9L, "admin@correo.com", "ADMIN", "ACTIVO");

        given(userRepo.findByIdWithStatusAndRoles(7L)).willReturn(Optional.of(userObjetivo));
        given(statusUserRepo.findByName("INACTIVO")).willReturn(Optional.of(status("INACTIVO")));
        given(authentication.getName()).willReturn("admin@correo.com");
        given(userRepo.findByEmail("admin@correo.com")).willReturn(Optional.of(admin));

        service.deleteUser(7L, null, authentication);

        ArgumentCaptor<UserReasonChange> captor = ArgumentCaptor.forClass(UserReasonChange.class);
        verify(userReasonChangeRepo).save(captor.capture());
        assertThat(captor.getValue().getReason()).isNull();
    }

    // ── F8-gerente/V38 ───────────────────────────────────────

    private void comoManager(String email, Long id) {
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn(email);
        org.mockito.Mockito.lenient().when(authentication.getAuthorities()).thenAnswer(inv -> List.of(
                (org.springframework.security.core.GrantedAuthority) () -> "ROLE_GERENTE"));
        User manager = user(id, email, "GERENTE", "ACTIVO");
        org.mockito.Mockito.lenient().when(userRepo.findByEmail(email)).thenReturn(Optional.of(manager));
    }

    @Test
    void manager_createReader_guardaCreatedBy() {
        comoManager("gerente@correo.com", 7L);
        given(roleRepo.findByName("LECTOR")).willReturn(Optional.of(role("LECTOR")));
        given(statusUserRepo.findByName("ACTIVO")).willReturn(Optional.of(status("ACTIVO")));
        given(userRepo.findByEmail("nuevo@correo.com")).willReturn(Optional.empty());
        given(userRepo.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });

        service.createUser(new com.uteq.backend.dto.CreateUserAdminRequestDTO(
                "Ana", "Paz", "nuevo@correo.com", "Secreta123", "LECTOR"), authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(7L);
    }

    @Test
    void manager_createManager_lanzaAccessDenied() {
        comoManager("gerente@correo.com", 7L);
        given(userRepo.findByEmail("otro@correo.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUser(new com.uteq.backend.dto.CreateUserAdminRequestDTO(
                "Ana", "Paz", "otro@correo.com", "Secreta123", "GERENTE"), authentication))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void manager_changeRoleOtroManager_lanzaAccessDenied() {
        comoManager("gerente@correo.com", 7L);
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        objetivo.setCreatedBy(99L);
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(roleRepo.findByName("BIBLIOTECARIO")).willReturn(Optional.of(role("BIBLIOTECARIO")));

        assertThatThrownBy(() -> service.changeRole(5L, "BIBLIOTECARIO", authentication))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void manager_blockSuCreated_permite() {
        comoManager("gerente@correo.com", 7L);
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        objetivo.setCreatedBy(7L);
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(statusUserRepo.findByName("INACTIVO")).willReturn(Optional.of(status("INACTIVO")));

        service.changeStatus(5L, "INACTIVO", "Baja", authentication);

        assertThat(objetivo.getStatus().getName()).isEqualTo("INACTIVO");
    }

    // ── listar: sobrecarga y alcance ─────────────────────────

    @Test
    void list3_sinAuth_delegaWithoutRestriccion() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(), pageable, 0);
        given(userRepo.searchWithFilters("", null, pageable)).willReturn(page);
        given(statusFineRepo.findByName("PENDIENTE")).willReturn(Optional.empty());

        Page<UserListingResponseDTO> result = service.list("", pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void list_managerScope_restringeByCreadorYSinPendientes() {
        comoManager("gerente@correo.com", 7L);
        org.mockito.Mockito.lenient().when(userRepo.findByEmailIgnoreCase("gerente@correo.com"))
                .thenReturn(Optional.of(user(7L, "gerente@correo.com", "GERENTE", "ACTIVO")));
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(), pageable, 0);
        given(userRepo.searchWithFilters("", 7L, pageable)).willReturn(page);
        given(statusFineRepo.findByName("PENDIENTE")).willReturn(Optional.empty());

        Page<UserListingResponseDTO> result = service.list(null, pageable, authentication, true);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void list_withFiltroTexto_trimAplica() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(), pageable, 0);
        given(userRepo.searchWithFilters("ana", null, pageable)).willReturn(page);
        given(statusFineRepo.findByName("PENDIENTE")).willReturn(Optional.empty());

        service.list("  ana  ", pageable, null, false);

        verify(userRepo).searchWithFilters("ana", null, pageable);
    }

    // ── cambiarRol: alcance gerente ──────────────────────────

    @Test
    void manager_changeRoleForbidden_lanzaAccessDenied() {
        comoManager("gerente@correo.com", 7L);
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        objetivo.setCreatedBy(7L);
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(roleRepo.findByName("ADMIN")).willReturn(Optional.of(role("ADMIN")));

        assertThatThrownBy(() -> service.changeRole(5L, "ADMIN", authentication))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void manager_changeRoleCreatedByNulo_lanzaAccessDenied() {
        comoManager("gerente@correo.com", 7L);
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        objetivo.setCreatedBy(null);
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(roleRepo.findByName("BIBLIOTECARIO")).willReturn(Optional.of(role("BIBLIOTECARIO")));

        assertThatThrownBy(() -> service.changeRole(5L, "BIBLIOTECARIO", authentication))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void manager_changeRoleSuCreated_permite() {
        comoManager("gerente@correo.com", 7L);
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        objetivo.setCreatedBy(7L);
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(roleRepo.findByName("BIBLIOTECARIO")).willReturn(Optional.of(role("BIBLIOTECARIO")));

        service.changeRole(5L, "BIBLIOTECARIO", authentication);

        assertThat(objetivo.getRoles()).extracting(Role::getName).containsExactly("BIBLIOTECARIO");
    }

    @Test
    void changeRole_authNula_lanzaEntityNotFound() {
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(roleRepo.findByName("BIBLIOTECARIO")).willReturn(Optional.of(role("BIBLIOTECARIO")));

        assertThatThrownBy(() -> service.changeRole(5L, "BIBLIOTECARIO", null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── cambiarEstado: alcance gerente ───────────────────────

    @Test
    void manager_changeStatusEstadoNoPermitido_lanzaAccessDenied() {
        comoManager("gerente@correo.com", 7L);
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        objetivo.setCreatedBy(7L);
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(statusUserRepo.findByName("BLOQUEADO_POR_MULTA"))
                .willReturn(Optional.of(status("BLOQUEADO_POR_MULTA")));

        assertThatThrownBy(() -> service.changeStatus(5L, "BLOQUEADO_POR_MULTA", "m", authentication))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void manager_changeStatusAjeno_lanzaAccessDenied() {
        comoManager("gerente@correo.com", 7L);
        User objetivo = user(5L, "lector@correo.com", "LECTOR", "ACTIVO");
        objetivo.setCreatedBy(99L);
        given(userRepo.findByIdWithStatusAndRoles(5L)).willReturn(Optional.of(objetivo));
        given(statusUserRepo.findByName("INACTIVO")).willReturn(Optional.of(status("INACTIVO")));

        assertThatThrownBy(() -> service.changeStatus(5L, "INACTIVO", "m", authentication))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    // ── crearUsuario: errores y happy admin ──────────────────

    @Test
    void createUser_emailDuplicado_lanzaAlreadyRegistered() {
        given(userRepo.findByEmail("dup@correo.com"))
                .willReturn(Optional.of(user(1L, "dup@correo.com", "LECTOR", "ACTIVO")));

        assertThatThrownBy(() -> service.createUser(new com.uteq.backend.dto.CreateUserAdminRequestDTO(
                "Ana", "Paz", "dup@correo.com", "Secreta123", "LECTOR"), authentication))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void createUser_roleInexistente_lanzaIllegalArgument() {
        given(userRepo.findByEmail("nuevo@correo.com")).willReturn(Optional.empty());
        given(roleRepo.findByName("SUPERVISOR")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUser(new com.uteq.backend.dto.CreateUserAdminRequestDTO(
                "Ana", "Paz", "nuevo@correo.com", "Secreta123", "SUPERVISOR"), authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }

    @Test
    void createUser_withoutEstadoActivo_lanzaIllegalState() {
        given(userRepo.findByEmail("nuevo@correo.com")).willReturn(Optional.empty());
        given(roleRepo.findByName("LECTOR")).willReturn(Optional.of(role("LECTOR")));
        given(statusUserRepo.findByName("ACTIVO")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUser(new com.uteq.backend.dto.CreateUserAdminRequestDTO(
                "Ana", "Paz", "nuevo@correo.com", "Secreta123", "LECTOR"), authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void admin_createReader_retornaResponse() {
        User admin = user(9L, "admin@correo.com", "ADMIN", "ACTIVO");
        given(userRepo.findByEmail("nuevo@correo.com")).willReturn(Optional.empty());
        given(roleRepo.findByName("LECTOR")).willReturn(Optional.of(role("LECTOR")));
        given(statusUserRepo.findByName("ACTIVO")).willReturn(Optional.of(status("ACTIVO")));
        given(authentication.getName()).willReturn("admin@correo.com");
        given(userRepo.findByEmail("admin@correo.com")).willReturn(Optional.of(admin));
        given(userRepo.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(60L);
            return u;
        });

        com.uteq.backend.dto.UserResponseDTO result = service.createUser(
                new com.uteq.backend.dto.CreateUserAdminRequestDTO(
                        "Ana", "Paz", "nuevo@correo.com", "Secreta123", "LECTOR"),
                authentication);

        assertThat(result.id()).isEqualTo(60L);
        assertThat(result.email()).isEqualTo("nuevo@correo.com");
    }

    // ── eliminarUsuario: errores ─────────────────────────────

    @Test
    void deleteUser_userInexistente_lanzaEntityNotFound() {
        given(userRepo.findByIdWithStatusAndRoles(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUser(404L, "m", authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteUser_withoutEstadoInactivo_lanzaIllegalState() {
        User objetivo = user(6L, "lector2@correo.com", "LECTOR", "ACTIVO");
        given(userRepo.findByIdWithStatusAndRoles(6L)).willReturn(Optional.of(objetivo));
        given(statusUserRepo.findByName("INACTIVO")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUser(6L, "m", authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── historial de motivos ─────────────────────────────────

    @Test
    void historyReasons_withRegistros_mapeaOrdenados() {
        UserReasonChange row = UserReasonChange.builder()
                .id(1L).userId(5L).typeChange("CAMBIO_ESTADO")
                .statusAnterior(3).statusFresh(2).reason("Baja")
                .executedBy(9L).created(java.time.OffsetDateTime.now())
                .build();
        given(userReasonChangeRepo.findByUserIdOrderByCreatedDesc(5L)).willReturn(List.of(row));

        List<com.uteq.backend.dto.UserReasonChangeResponseDTO> result = service.historyReasons(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).reason()).isEqualTo("Baja");
    }

    @Test
    void historyReasons_withoutRegistros_retornaVacio() {
        given(userReasonChangeRepo.findByUserIdOrderByCreatedDesc(5L)).willReturn(List.of());

        assertThat(service.historyReasons(5L)).isEmpty();
    }
}
