package com.uteq.backend.security;

import com.uteq.backend.entity.Role;
import com.uteq.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Servicio de usuarios para Spring Security: carga por correo, mapea los roles de la
 * base sin prefijo y traduce el estado a cuenta bloqueada o deshabilitada.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    // estados_usuario.nombre que restringen el login. BLOQUEADO_POR_MULTA
    // se modela como cuenta bloqueada (accountLocked=true -> isAccountNonLocked()
    // = false, el hook de Spring Security pensado para restricciones
    // reversibles/temporales); INACTIVO y PENDIENTE_VERIFICACION como
    // cuenta deshabilitada (disabled=true -> isEnabled()=false). Con esto
    // Spring Security ya rechaza el login (LockedException / DisabledException)
    // sin necesidad de lógica custom en AuthService.
    private static final String ESTADO_BLOQUEADO_POR_MULTA = "BLOQUEADO_POR_MULTA";
    private static final Set<String> ESTADOS_DESHABILITADOS = Set.of("INACTIVO", "PENDIENTE_VERIFICACION");

    private final UserRepository userRepository;
    /**
     * Carga el usuario por correo con sus roles y su estado.
     * Marca la cuenta como bloqueada si está {@code BLOQUEADO_POR_MULTA} y como
     * deshabilitada si está {@code INACTIVO} o {@code PENDIENTE_VERIFICACION}.
     *
     * @param email correo del usuario a cargar
     * @return datos del usuario con roles y flags de estado para Spring Security
     * @throws UsernameNotFoundException si no existe un usuario con ese correo
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.uteq.backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + email));

        String statusName = user.getStatus().getName();

        // roles.nombre en la BD no lleva prefijo ("LECTOR", no "ROLE_LECTOR").
        // User.builder().roles(...) de Spring Security antepone "ROLE_"
        // automáticamente -- justo lo que esperan los @PreAuthorize
        // ("hasAnyRole('LECTOR', ...)") ya existentes en LibroController,
        // que también comparan anteponiendo "ROLE_" a lo que reciben.
        // Ambos lados deben coincidir en anteponer el prefijo una sola vez.
        String[] roles = user.getRoles().stream()
                .map(Role::getName)
                .toArray(String[]::new);

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(roles)
                .accountLocked(ESTADO_BLOQUEADO_POR_MULTA.equals(statusName))
                .disabled(ESTADOS_DESHABILITADOS.contains(statusName))
                .build();
    }
}
