package com.uteq.backend.security;

import com.uteq.backend.entity.Role;
import com.uteq.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Servicio JWT con firma HS256: genera tokens de acceso y refresco, valida firma y
 * expiración, y extrae el correo, el identificador y la expiración desde los claims.
 */
@Service
public class JwtService {

    // Orden de mayor a menor privilegio, usado solo para elegir el claim
    // "rol" (singular) de retrocompatibilidad -- ver comentario en
    // buildToken(). No afecta autorización real: JwtAuthFilter reconstruye
    // las authorities en cada request vía UserDetailsServiceImpl (consulta
    // fresca a la BD), nunca lee claims de rol del propio JWT.
    private static final List<String> JERARQUIA_ROLES = List.of("ADMIN", "GERENTE", "BIBLIOTECARIO", "LECTOR");

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Devuelve el tiempo de vida del token de acceso en milisegundos.
     *
     * @return expiración del token de acceso en milisegundos
     */

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Devuelve el tiempo de vida del token de refresco en milisegundos.
     *
     * @return expiración del token de refresco en milisegundos
     */

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
         * Genera un JWT de acceso para un usuario.
     * @param user usuario para el token
     * @return JWT firmado
     * @throws RuntimeException si falla la firma
     */

    public String generateToken(User user) {
        return buildToken(user, expirationMs);
    }

    /**
     * Genera un JWT de refresco con el tiempo de vida de refresco.
     *
     * @param user usuario para el token de refresco
     * @return JWT de refresco firmado
     */

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs);
    }

    private String buildToken(User user, long ttlMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlMs);

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("correo", user.getEmail())
                // Claim real para autorización: arreglo completo de roles.
                .claim("roles", roles)
                // DECISIÓN TEMPORAL DE RETROCOMPATIBILIDAD (TAREA 4.1): se
                // conserva el claim singular "rol" (el de mayor privilegio
                // según JERARQUIA_ROLES) solo por si algún código frontend
                // ya existente lo lee. Eliminar este claim cuando el
                // frontend (Panama) migre a leer "roles" (array) en su
                // lugar -- no agregar más lógica que dependa de "rol"
                // mientras tanto.
                .claim("rol", rolePrincipal(roles))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private String rolePrincipal(List<String> roles) {
        return roles.stream()
                .min(Comparator.comparingInt(name -> {
                    int idx = JERARQUIA_ROLES.indexOf(name);
                    return idx == -1 ? Integer.MAX_VALUE : idx;
                }))
                .orElse(null);
    }

    /**
         * Valida la firma y expiracion de un JWT.
     * @param token JWT a validar
     * @return true si valido y no expirado
     */

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extrae el correo del claim {@code correo} del token.
     *
     * @param token JWT firmado a leer
     * @return correo contenido en el token
     */

    public String extractEmail(String token) {
        return extractClaims(token).get("correo", String.class);
    }

    /**
     * Extrae el identificador único ({@code jti}) del token.
     *
     * @param token JWT firmado a leer
     * @return identificador único del token
     */

    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    /**
     * Extrae la fecha de expiración del token.
     *
     * @param token JWT firmado a leer
     * @return fecha de expiración contenida en el token
     */

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
