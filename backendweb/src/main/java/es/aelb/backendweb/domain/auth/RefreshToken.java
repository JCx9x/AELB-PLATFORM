package es.aelb.backendweb.domain.auth;

import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sesión de refresco. Solo se persiste el hash SHA-256 del token — el valor
 * en claro únicamente existe en el cliente (cookie httpOnly) y en la
 * respuesta inmediata al emitirlo.
 *
 * Rotación: cada uso revoca el token actual y emite uno nuevo. Un intento de
 * reutilizar un token ya revocado indica robo y el llamador debe revocar
 * todas las sesiones del usuario (ver RefreshTokenRepository.revokeAllForUser).
 */
public class RefreshToken extends AggregateRoot<String> {

    private final UserId        userId;
    private final String        tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private       LocalDateTime revokedAt;

    private RefreshToken(String id, UserId userId, String tokenHash,
                          LocalDateTime expiresAt, LocalDateTime createdAt, LocalDateTime revokedAt) {
        super(id);
        this.userId    = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(UserId userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(UUID.randomUUID().toString(), userId, tokenHash, expiresAt, LocalDateTime.now(), null);
    }

    public static RefreshToken reconstitute(String id, UserId userId, String tokenHash,
                                             LocalDateTime expiresAt, LocalDateTime createdAt, LocalDateTime revokedAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, createdAt, revokedAt);
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now();
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isRevoked() && !isExpired();
    }

    public UserId        getUserId()    { return userId; }
    public String        getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
}
