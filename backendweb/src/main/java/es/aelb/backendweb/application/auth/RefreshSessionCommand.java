package es.aelb.backendweb.application.auth;

public record RefreshSessionCommand(String rawRefreshToken) {}
