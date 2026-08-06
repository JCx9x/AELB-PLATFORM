package es.aelb.backendweb.application.registration;

public record UpdatePaymentStatusCommand(
        String registrationId,
        String newStatus
) {}
