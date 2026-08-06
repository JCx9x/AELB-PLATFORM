package es.aelb.backendweb.application.auth;

import java.time.LocalDateTime;

/** rawToken solo existe aquí y en la cookie del cliente — nunca se persiste en claro. */
public record IssuedRefreshToken(String rawToken, LocalDateTime expiresAt) {}
