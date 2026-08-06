package es.aelb.backendweb.domain.auth;

import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.util.Optional;

public interface RefreshTokenRepository {

    void save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoca todas las sesiones de refresco activas del usuario (robo detectado, logout global, bloqueo). */
    void revokeAllForUser(UserId userId);
}
