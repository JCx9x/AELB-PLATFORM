package es.aelb.backendweb.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Generación y validación de JWT usando JJWT 0.12.x.
 * El token lleva: jti, sub (userId), email, role (informativo — la
 * autorización real usa el rol vivo de BD, ver JwtAuthenticationFilter),
 * ver (versión de sesión del usuario), iat, exp.
 */
@Component
public class JwtTokenProvider {

    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "insecure_default_change_me_in_production",
            "change_me_jwt_secret_min_32_chars_long"
    );

    @Value("${aelb.jwt.secret}")
    private String secret;

    @Value("${aelb.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * A known or weak signing key turns JWT authentication into privilege
     * escalation. Fail closed during startup instead of using a fallback.
     */
    @PostConstruct
    void validateConfiguration() {
        validateSecret(secret);
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS debe ser mayor que cero");
        }
    }

    static void validateSecret(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET es obligatorio");
        }

        if (FORBIDDEN_SECRETS.contains(configuredSecret)) {
            throw new IllegalStateException("JWT_SECRET utiliza un valor inseguro conocido");
        }

        if (configuredSecret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes aleatorios");
        }
    }

    public String generateToken(String userId, String email, String role, int tokenVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim("ver", tokenVersion)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    /** -1 si el token no lleva claim de versión (formato anterior a esta migración) — fuerza el rechazo. */
    public int getTokenVersion(Claims claims) {
        Integer version = claims.get("ver", Integer.class);
        return version != null ? version : -1;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
