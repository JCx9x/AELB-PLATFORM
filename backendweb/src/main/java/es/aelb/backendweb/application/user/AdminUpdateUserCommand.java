package es.aelb.backendweb.application.user;

public record AdminUpdateUserCommand(
        String  userId,
        String  firstName,
        String  lastName,
        String  phone,
        String  birthDate,   // ISO date string, obligatorio
        String  role,        // nullable = don't change
        Boolean blocked      // nullable = don't change
) {}
