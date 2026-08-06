package es.aelb.backendweb.application.user;

import java.time.LocalDate;

public record UpdateProfileCommand(
        String    userId,
        String    firstName,
        String    lastName,
        String    phone,
        LocalDate birthDate
) {}
