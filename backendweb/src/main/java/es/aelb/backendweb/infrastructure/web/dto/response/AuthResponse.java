package es.aelb.backendweb.infrastructure.web.dto.response;

public record AuthResponse(
        String userId,
        String email,
        String role,
        String firstName,
        String lastName
) {}
