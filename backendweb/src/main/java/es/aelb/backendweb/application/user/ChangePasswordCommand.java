package es.aelb.backendweb.application.user;

public record ChangePasswordCommand(
        String userId,
        String currentPassword,
        String newPassword
) {}
