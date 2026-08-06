package es.aelb.backendweb.application.user;

public record LoginResult(
        String userId,
        String email,
        String role,
        String firstName,
        String lastName,
        int    tokenVersion
) {}
