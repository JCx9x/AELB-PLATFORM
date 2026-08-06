package es.aelb.backendweb.application.registration;

public record CancelRegistrationCommand(
        String registrationId,
        String callerUserId,
        boolean callerIsPrivileged   // true if GESTOR or ADMIN
) {}
