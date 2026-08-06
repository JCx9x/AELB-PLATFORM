package es.aelb.backendweb.application.user;

import java.time.LocalDate;

public record CreateUserCommand(
        String    email,
        String    rawPassword,
        String    firstName,
        String    lastName,
        String    dni,
        String    phone,
        String    city,
        String    province,
        String    nationality,
        LocalDate birthDate
) {}
