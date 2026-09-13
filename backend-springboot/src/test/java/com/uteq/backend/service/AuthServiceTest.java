package com.uteq.backend.service;

import com.uteq.backend.dto.LoginRequestDTO;
import com.uteq.backend.dto.RegistrationRequestDTO;
import com.uteq.backend.dto.TokenResponseDTO;
import com.uteq.backend.entity.StatusUser;
import com.uteq.backend.entity.Role;
import com.uteq.backend.entity.User;
import com.uteq.backend.repository.AuditLogAuditRepository;
import com.uteq.backend.repository.StatusUserRepository;
import com.uteq.backend.repository.RoleRepository;
import com.uteq.backend.repository.UserRepository;
import com.uteq.backend.security.JwtService;
import com.uteq.backend.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String IP_DE_PRUEBA = "10.0.0.1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StatusUserRepository statusUserRepository;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private AuditLogAuditRepository auditLogAuditRepository;

    @Mock
    private VerificationEmailService verificationEmailService;

    @Mock
    private ConfigurationSystemService configurationSystemService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User userTest() {
        Instant ahora = Instant.now();

        StatusUser active = new StatusUser();
        active.setId(1);
        active.setName("ACTIVO");

        Role reader = new Role();
        reader.setId(1);
        reader.setName("LECTOR");

        return User.builder()
                .id(1L)
                .name("Lector de Prueba")
                .lastName("Apellido de Prueba")
                .email("lector@correo.com")
                .passwordHash("hash-encriptado")
                .status(active)
                .emailVerified(true)
                .roles(Set.of(reader))
                .dateRegistration(ahora)
                .updated(ahora)
                .build();
    }

    @Test
    void loginSuccessful() {
        User user = userTest();
        LoginRequestDTO dto = new LoginRequestDTO("lector@correo.com", "password123");

        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token-de-prueba");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token-de-prueba");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        TokenResponseDTO result = authService.login(dto, IP_DE_PRUEBA);

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertFalse(result.accessToken().isBlank());
        assertNotNull(result.refreshToken());
        assertFalse(result.refreshToken().isBlank());
        assertEquals(3600L, result.expiresIn());
        assertEquals("Bearer", result.tokenType());
    }

    // OWASP A07: un login exitoso debe resetear el contador de intentos
    // fallidos de esa combinación correo+IP -- si no, un usuario que se
    // equivocó una vez y luego acertó seguiría acumulando hacia el bloqueo.
    @Test
    void loginSuccessfulReseteaContadorRateLimit() {
        User user = userTest();
        LoginRequestDTO dto = new LoginRequestDTO("lector@correo.com", "password123");

        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token-de-prueba");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token-de-prueba");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        authService.login(dto, IP_DE_PRUEBA);

        verify(loginRateLimiter).reset("lector@correo.com", IP_DE_PRUEBA);
        verify(auditLogAuditRepository).save(any());
    }

    @Test
    void loginKeyIncorrecta() {
        LoginRequestDTO dto = new LoginRequestDTO("lector@correo.com", "claveIncorrecta");

        doThrow(new BadCredentialsException("Credenciales inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(dto, IP_DE_PRUEBA));
    }

    // OWASP A07: cada intento fallido debe incrementar el contador de esa
    // combinación correo+IP -- sin esto, LoginRateLimiter.estaBloqueado()
    // nunca llegaría al máximo configurado.
    @Test
    void loginFailedIncrementaContadorRateLimit() {
        LoginRequestDTO dto = new LoginRequestDTO("lector@correo.com", "claveIncorrecta");

        doThrow(new BadCredentialsException("Credenciales inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(dto, IP_DE_PRUEBA));

        verify(loginRateLimiter).registerFailure("lector@correo.com", IP_DE_PRUEBA);
        verify(auditLogAuditRepository).save(any());
    }

    // OWASP A07: el escenario del 6to intento -- LoginRateLimiter ya
    // reporta la combinación correo+IP como bloqueada (equivalente a un
    // contador que llegó al máximo configurado, ver LoginRateLimiterTest
    // para el límite en sí). AuthService debe responder con la excepción
    // que GlobalExceptionHandler traduce a 429 SIN llamar a
    // authenticationManager.authenticate() -- ni siquiera se intenta
    // autenticar contra credenciales potencialmente correctas.
    @Test
    void loginBlockedByRateLimitNotIntentaAutenticar() {
        LoginRequestDTO dto = new LoginRequestDTO("lector@correo.com", "password123");

        when(loginRateLimiter.isBlocked("lector@correo.com", IP_DE_PRUEBA)).thenReturn(true);
        when(loginRateLimiter.remainingSeconds("lector@correo.com", IP_DE_PRUEBA)).thenReturn(600L);

        assertThrows(LoginRateLimitExceededException.class, () -> authService.login(dto, IP_DE_PRUEBA));

        verify(authenticationManager, never()).authenticate(any());
        verify(loginRateLimiter, never()).registerFailure(any(), any());
    }

    @Test
    void registrationEmailDuplicate() {
        User userExisting = userTest();
        RegistrationRequestDTO dto = new RegistrationRequestDTO(
                "Nuevo Lector", "Apellido Nuevo", "lector@correo.com", "password123"
        );

        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(userExisting));

        assertThrows(EmailAlreadyRegisteredException.class, () -> authService.register(dto));

        verify(userRepository, never()).save(any());
    }

    // Módulo 9.5: ya no ACTIVO directo -- ver AuthService.ESTADO_INICIAL.
    @Test
    void registrationSuccessful_dejaUserPendingVerificationYEnviaCode() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(
                "Nuevo", "Lector", "nuevo@correo.com", "password123");

        StatusUser pendingVerification = new StatusUser();
        pendingVerification.setId(4);
        pendingVerification.setName("PENDIENTE_VERIFICACION");

        Role reader = new Role();
        reader.setId(1);
        reader.setName("LECTOR");

        when(userRepository.findByEmail("nuevo@correo.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("LECTOR")).thenReturn(Optional.of(reader));
        when(statusUserRepository.findByName("PENDIENTE_VERIFICACION")).thenReturn(Optional.of(pendingVerification));
        when(passwordEncoder.encode("password123")).thenReturn("hash-encriptado");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        authService.register(dto);

        var capturado = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(capturado.capture());
        assertEquals("PENDIENTE_VERIFICACION", capturado.getValue().getStatus().getName());
        assertFalse(capturado.getValue().isEmailVerified());
        verify(verificationEmailService).generateAndSendCode(any(User.class));
    }

    // REQ-NF-013 / OWASP A03: replica como test de regresión permanente el
    // caso 2 de docs/mediciones/sec/2026-07-30-owasp-a03-inyeccion.md
    // (verificado antes solo de forma manual con curl contra Docker real,
    // sin test en el suite). El payload clásico `' OR '1'='1` y un intento
    // de `DROP TABLE` en campos de texto libre (nombre/apellido) deben
    // guardarse como texto literal -- si UsuarioRepository.save() usara
    // concatenación de SQL en vez de un PreparedStatement parametrizado,
    // este flujo lanzaría una excepción en vez de completar el registro.
    @Test
    void registrationWithPayloadInyeccionSql_seGuardaComoTextLiteralWithoutLanzarException() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(
                "' OR '1'='1", "'; DROP TABLE usuarios; --", "owasp@correo.com", "password123");

        StatusUser pendingVerification = new StatusUser();
        pendingVerification.setId(4);
        pendingVerification.setName("PENDIENTE_VERIFICACION");

        Role reader = new Role();
        reader.setId(1);
        reader.setName("LECTOR");

        when(userRepository.findByEmail("owasp@correo.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("LECTOR")).thenReturn(Optional.of(reader));
        when(statusUserRepository.findByName("PENDIENTE_VERIFICACION")).thenReturn(Optional.of(pendingVerification));
        when(passwordEncoder.encode("password123")).thenReturn("hash-encriptado");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        authService.register(dto);

        var capturado = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(capturado.capture());
        assertEquals("' OR '1'='1", capturado.getValue().getName());
        assertEquals("'; DROP TABLE usuarios; --", capturado.getValue().getLastName());
    }

    @Test
    void registrationWithDomainAllowed_continueRegistration() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(
                "Nuevo", "Lector", "nuevo@correo.com", "password123");
        StatusUser pendingVerification = new StatusUser();
        pendingVerification.setId(4);
        pendingVerification.setName("PENDIENTE_VERIFICACION");
        Role reader = new Role();
        reader.setId(1);
        reader.setName("LECTOR");

        when(configurationSystemService.getValue("correo_dominios_permitidos"))
                .thenReturn("uteq.edu.ec, correo.com");
        when(userRepository.findByEmail("nuevo@correo.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("LECTOR")).thenReturn(Optional.of(reader));
        when(statusUserRepository.findByName("PENDIENTE_VERIFICACION")).thenReturn(Optional.of(pendingVerification));
        when(passwordEncoder.encode("password123")).thenReturn("hash-encriptado");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });

        authService.register(dto);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registrationWithDomainNotAllowed_lanzaDomainException() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(
                "Nuevo", "Lector", "nuevo@example.com", "password123");
        when(userRepository.findByEmail("nuevo@example.com")).thenReturn(Optional.empty());
        when(configurationSystemService.getValue("correo_dominios_permitidos"))
                .thenReturn("uteq.edu.ec, correo.com");

        assertThrows(EmailDomainNotAllowedException.class, () -> authService.register(dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registrationWithoutConfigDomain_noRestrictsRegistration() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(
                "Nuevo", "Lector", "nuevo@example.com", "password123");
        StatusUser pendingVerification = new StatusUser();
        pendingVerification.setId(4);
        pendingVerification.setName("PENDIENTE_VERIFICACION");
        Role reader = new Role();
        reader.setId(1);
        reader.setName("LECTOR");

        when(userRepository.findByEmail("nuevo@example.com")).thenReturn(Optional.empty());
        when(configurationSystemService.getValue("correo_dominios_permitidos"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("sin clave"));
        when(roleRepository.findByName("LECTOR")).thenReturn(Optional.of(reader));
        when(statusUserRepository.findByName("PENDIENTE_VERIFICACION")).thenReturn(Optional.of(pendingVerification));
        when(passwordEncoder.encode("password123")).thenReturn("hash-encriptado");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(102L);
            return u;
        });

        authService.register(dto);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyEmail_codeValid_activeUserYMarkEmailVerified() {
        User userPending = userTest();
        userPending.setEmailVerified(false);

        StatusUser active = new StatusUser();
        active.setId(1);
        active.setName("ACTIVO");

        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(userPending));
        when(statusUserRepository.findByName("ACTIVO")).thenReturn(Optional.of(active));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.verifyEmail("lector@correo.com", "123456", IP_DE_PRUEBA);

        verify(verificationEmailService).validate("lector@correo.com", "123456");
        var capturado = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(capturado.capture());
        assertTrue(capturado.getValue().isEmailVerified());
        verify(auditLogAuditRepository).save(any());
    }

    // El código inválido/expirado lo detecta VerificacionCorreoService --
    // AuthService no debe intentar activar al usuario si esa validación
    // lanza.
    @Test
    void verifyEmail_codeInvalid_notActiveUser() {
        doThrow(new CodeVerificationInvalidException("El código ingresado es incorrecto."))
                .when(verificationEmailService).validate("lector@correo.com", "000000");

        assertThrows(CodeVerificationInvalidException.class,
                () -> authService.verifyEmail("lector@correo.com", "000000", IP_DE_PRUEBA));

        verify(userRepository, never()).save(any());
    }

    @Test
    void logoutGuardaTokenBlacklist() {
        String token = "token-de-prueba";
        String jti = "550e8400-e29b-41d4-a716-446655440000";
        Date expiracionFutura = new Date(System.currentTimeMillis() + 3600000);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtService.extractJti(token)).thenReturn(jti);
        when(jwtService.extractExpiration(token)).thenReturn(expiracionFutura);

        authService.logout(token, IP_DE_PRUEBA);

        verify(valueOperations).set(eq("blacklist:" + jti), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
        verify(auditLogAuditRepository).save(any());
    }

    @Test
    void logoutTokenExpired_noGuardaBlacklist() {
        String token = "token-expirado";
        when(jwtService.extractJti(token)).thenReturn("jti-expirado");
        when(jwtService.extractExpiration(token)).thenReturn(new Date(System.currentTimeMillis() - 1000));
        when(jwtService.extractEmail(token)).thenReturn("lector@correo.com");

        authService.logout(token, IP_DE_PRUEBA);

        verify(redisTemplate, never()).opsForValue();
        verify(auditLogAuditRepository).save(any());
    }

    @Test
    void logoutRedisCaido_noRompeLogout() {
        String token = "token-de-prueba";
        String jti = "550e8400-e29b-41d4-a716-446655440000";
        Date expiracionFutura = new Date(System.currentTimeMillis() + 3600000);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtService.extractJti(token)).thenReturn(jti);
        when(jwtService.extractExpiration(token)).thenReturn(expiracionFutura);
        when(jwtService.extractEmail(token)).thenReturn("lector@correo.com");
        doThrow(new DataAccessResourceFailureException("redis caido"))
                .when(valueOperations).set(eq("blacklist:" + jti), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));

        authService.logout(token, IP_DE_PRUEBA);

        verify(auditLogAuditRepository).save(any());
    }

    @Test
    void resendCode_emailAlreadyVerified_lanzaIllegalArgument() {
        User user = userTest();
        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> authService.resendCode("lector@correo.com"));

        verify(verificationEmailService, never()).generateAndSendCode(any());
    }

    @Test
    void resendCode_pendingAccount_regeneraCodigo() {
        User user = userTest();
        user.setEmailVerified(false);
        StatusUser pendingVerification = new StatusUser();
        pendingVerification.setName("PENDIENTE_VERIFICACION");
        user.setStatus(pendingVerification);
        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(user));

        authService.resendCode("lector@correo.com");

        verify(verificationEmailService).generateAndSendCode(user);
    }

    @Test
    void requestReset_redisCaido_lanzaServiceUnavailable() {
        User user = userTest();
        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new DataAccessResourceFailureException("redis caido"))
                .when(valueOperations).set(eq("reset-codigo:lector@correo.com"), any(), any(java.time.Duration.class));

        assertThrows(ServiceTemporarilyNotAvailableException.class,
                () -> authService.requestReset("lector@correo.com"));
    }

    @Test
    void requestReset_emailServiceFails_keepsResetCode() {
        User user = userTest();
        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(emailService.sendEmail(eq("lector@correo.com"), any(), any())).thenReturn(false);

        authService.requestReset("lector@correo.com");

        verify(valueOperations).set(eq("reset-codigo:lector@correo.com"), any(), any(java.time.Duration.class));
        verify(emailService).sendEmail(eq("lector@correo.com"), any(), any());
    }

    @Test
    void resetPassword_codeInvalid_lanzaCodeVerificationInvalid() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("reset-codigo:lector@correo.com")).thenReturn("123456");

        assertThrows(CodeVerificationInvalidException.class,
                () -> authService.resetPassword("lector@correo.com", "000000", "nuevaClave"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_deleteRedisFails_passwordAlreadyUpdated() {
        User user = userTest();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("reset-codigo:lector@correo.com")).thenReturn("123456");
        when(userRepository.findByEmail("lector@correo.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nuevaClave")).thenReturn("hash-nuevo");
        doThrow(new DataAccessResourceFailureException("redis caido"))
                .when(redisTemplate).delete("reset-codigo:lector@correo.com");

        authService.resetPassword("lector@correo.com", "123456", "nuevaClave");

        verify(userRepository).save(user);
        assertEquals("hash-nuevo", user.getPasswordHash());
    }

    @Test
    void refreshWithTokenValid() {
        String refreshTokenTest = "refresh-token-de-prueba";
        User user = userTest();

        when(jwtService.validateToken(refreshTokenTest)).thenReturn(true);
        when(jwtService.extractEmail(refreshTokenTest)).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("nuevo-access-token");

        TokenResponseDTO result = authService.refresh(refreshTokenTest);

        assertEquals("nuevo-access-token", result.accessToken());
        assertSame(refreshTokenTest, result.refreshToken());
    }

    // Antes de este fix, un refreshToken invalido/expirado lanzaba una
    // RuntimeException generica que GlobalExceptionHandler no capturaba de
    // forma especifica -- caia en el handler generico y respondia 500 en
    // vez de 401. Ver GlobalExceptionHandler.handleRefreshTokenInvalido.
    @Test
    void refreshWithTokenInvalid_lanzaRefreshTokenInvalidException() {
        String refreshTokenTest = "refresh-token-invalido";

        when(jwtService.validateToken(refreshTokenTest)).thenReturn(false);

        assertThrows(RefreshTokenInvalidException.class, () -> authService.refresh(refreshTokenTest));

        verify(userRepository, never()).findByEmail(any());
    }

    // Caso borde: el token es valido (firma/expiracion correctas) pero el
    // correo que codifica ya no resuelve a un usuario existente (ej. cuenta
    // eliminada). Mismo tratamiento que un token invalido -- 401, no 500.
    @Test
    void refreshWithUserNotFound_lanzaRefreshTokenInvalidException() {
        String refreshTokenTest = "refresh-token-de-usuario-eliminado";

        when(jwtService.validateToken(refreshTokenTest)).thenReturn(true);
        when(jwtService.extractEmail(refreshTokenTest)).thenReturn("fantasma@correo.com");
        when(userRepository.findByEmail("fantasma@correo.com")).thenReturn(Optional.empty());

        assertThrows(RefreshTokenInvalidException.class, () -> authService.refresh(refreshTokenTest));
    }
}
