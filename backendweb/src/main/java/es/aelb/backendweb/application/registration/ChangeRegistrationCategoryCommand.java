package es.aelb.backendweb.application.registration;

public record ChangeRegistrationCategoryCommand(
        String registrationId,
        String newCategoryId
) {}
