package com.uteq.backend.controller;

import com.uteq.backend.dto.CodeVerificationRequestDTO;
import com.uteq.backend.dto.LoginRequestDTO;
import com.uteq.backend.dto.ResendCodeRequestDTO;
import com.uteq.backend.dto.RegistrationRequestDTO;
import com.uteq.backend.dto.ResetPasswordRequestDTO;
import com.uteq.backend.dto.RequestResetRequestDTO;
import com.uteq.backend.dto.TokenResponseDTO;
import com.uteq.backend.dto.UserResponseDTO;
import com.uteq.backend.config.RefreshCookieConfig;
import com.uteq.backend.security.JwtService;
import com.uteq.backend.service.AuthService;
import com.uteq.backend.service.ServiceTemporarilyNotAvailableException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Autenticación pública pre-login: registro, verificación, login,
 * refresh por cookie HttpOnly, logout y recuperación de contraseña.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshCookieConfig.RefreshCookiePolicy cookiePolicy;

    /**
     * Constructor con los servicios de autenticación y JWT.
     *
     * @param authService servicio de registro, login y tokens
     * @param jwtService servicio de emisión y validación de JWT
     * @param cookiePolicy política de la cookie refreshToken (Secure en
     *        prod; relajada solo bajo el perfil dev-local-http)
     */
    public AuthController(AuthService authService, JwtService jwtService,
            RefreshCookieConfig.RefreshCookiePolicy cookiePolicy) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookiePolicy = cookiePolicy;
    }
    /**
     * Procesa registration y devuelve el resultado calculado por el backend.
     *
     * @param dto datos validados de la peticion con la informacion necesaria para ejecutar la operacion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @PostMapping("/registro")
    public ResponseEntity<UserResponseDTO> registration(@Valid @RequestBody RegistrationRequestDTO dto) {
        UserResponseDTO user = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // Sin JWT: el usuario aún no puede loguearse (PENDIENTE_VERIFICACION).
    // Regenera el código de 6 dígitos en Redis cuando el anterior expiró
    // (TTL 10 min) y el usuario quedó bloqueado sin intervención de ADMIN.
    /**
         * Reenvia el codigo de verificacion para una cuenta pendiente.
     * @param dto email de la cuenta
     * @return ResponseEntity vacia
     * @throws EntityNotFoundException si email no existe
     * @throws IllegalArgumentException si ya verificado
     */
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Void> resendCode(@Valid @RequestBody ResendCodeRequestDTO dto) {
        authService.resendCode(dto.email());
        return ResponseEntity.noContent().build();
    }
    /**
         * Inicia recuperacion de contrasena enviando codigo por email.
     * @param dto email de la cuenta
     * @return ResponseEntity vacia
     * @throws EntityNotFoundException si email no existe
     * @throws ServiceTemporarilyNotAvailableException si Redis no disponible
     */
    @PostMapping("/solicitar-reset")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody RequestResetRequestDTO dto) {
        authService.requestReset(dto.email());
        return ResponseEntity.noContent().build();
    }

    /**
     * Procesa reset y devuelve el resultado calculado por el backend.
     *
     * @param dto datos validados de la peticion con la informacion necesaria para ejecutar la operacion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        authService.resetPassword(dto.email(), dto.code(), dto.freshPassword());
        return ResponseEntity.noContent().build();
    }

    // Sin JWT: el recién registrado aún no puede loguearse.
    // La identidad se prueba con el código de un solo uso.
    /**
         * Verifica codigo de activacion y activa la cuenta.
     * @param dto email y codigo de verificacion
     * @param request HTTP request para IP
     * @return ResponseEntity con UserResponseDTO activado
     * @throws IllegalArgumentException si codigo invalido/expirado
     */
    @PostMapping("/verificar-correo")
    public ResponseEntity<UserResponseDTO> verifyEmail(
            @Valid @RequestBody CodeVerificationRequestDTO dto, HttpServletRequest request) {
        UserResponseDTO user = authService.verifyEmail(dto.email(), dto.code(), getIpSource(request));
        return ResponseEntity.ok(user);
    }
    /**
     * Procesa login y devuelve el resultado calculado por el backend.
     *
     * @param dto datos validados de la peticion con la informacion necesaria para ejecutar la operacion
     * @param request datos validados de la peticion con la informacion necesaria para ejecutar la operacion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        TokenResponseDTO tokens = authService.login(dto, getIpSource(request));
        ResponseCookie cookie = buildRefreshCookie(tokens.refreshToken(), jwtService.getRefreshExpirationMs());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens);
    }
    /**
     * Procesa logout y devuelve el resultado calculado por el backend.
     *
     * @param authHeader token de seguridad recibido para validar o renovar la sesion del usuario
     * @param request datos validados de la peticion con la informacion necesaria para ejecutar la operacion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader, HttpServletRequest request) {
        String token = authHeader.substring(BEARER_PREFIX.length());
        authService.logout(token, getIpSource(request));
        ResponseCookie cookieLimpia = buildRefreshCookie("", 0);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieLimpia.toString())
                .build();
    }

    // IP real del cliente para rate limit y auditoría. Lee getRemoteAddr();
    // no usa X-Forwarded-For por ser falsificable sin proxy de confianza.
    private String getIpSource(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    // El refresh token viaja SOLO en la cookie HttpOnly (nunca en el body):
    // así JS del frontend no puede leerlo ni reenviarlo manualmente, que es
    // precisamente el punto de HttpOnly. required=false + validación manual
    // (en vez de required=true) para que la ausencia de cookie caiga en el
    // handler ya existente de IllegalArgumentException (400, RFC 7807) en
    // vez de en el mecanismo de error por defecto de Spring MVC.
    /**
     * Procesa refresh y devuelve el resultado calculado por el backend.
     *
     * @param refreshTokenCookie token de seguridad recibido para validar o renovar la sesion del usuario
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new IllegalArgumentException("Falta la cookie " + REFRESH_COOKIE_NAME);
        }

        TokenResponseDTO tokens = authService.refresh(refreshTokenCookie);
        ResponseCookie cookie = buildRefreshCookie(tokens.refreshToken(), jwtService.getRefreshExpirationMs());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens);
    }

    // path="/api/auth" limita el envío de la cookie a los endpoints de
    // autenticación (nunca viaja en llamadas a /api/v1/**). Ver
    // docs/adr/adr-007-cookies-jwt.md para el resto de decisiones de diseño
    // (por qué solo el refreshToken migra a cookie, no el accessToken).
    // La dureza Secure+SameSite=None vive en RefreshCookieConfig (siempre
    // en prod; excepción solo bajo el perfil dev-local-http).
    private ResponseCookie buildRefreshCookie(String value, long maxAgeMs) {
        return cookiePolicy.build(REFRESH_COOKIE_NAME, value, Duration.ofMillis(maxAgeMs));
    }
}
