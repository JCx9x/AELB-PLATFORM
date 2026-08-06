package es.aelb.backendweb.application.auth;

import java.time.LocalDateTime;

public record RefreshSessionResult(
        String        userId,
        String        email,
        String        role,
        String        firstName,
        String        lastName,
        int           tokenVersion,
        String        newRawRefreshToken,
        LocalDateTime newRefreshExpiresAt
) {}
