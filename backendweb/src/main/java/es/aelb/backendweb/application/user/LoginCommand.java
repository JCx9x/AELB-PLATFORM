package es.aelb.backendweb.application.user;

public record LoginCommand(String email, String rawPassword) {}
