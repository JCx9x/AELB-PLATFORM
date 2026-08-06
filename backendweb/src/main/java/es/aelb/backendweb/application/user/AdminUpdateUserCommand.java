package es.aelb.backendweb.application.user;

public record AdminUpdateUserCommand(
        String  userId,
        String  firstName,
        String  lastName,
        String  phone,
        String  birthDate,   // ISO date string, nullable
        String  role,        // nullable = don't change
        Boolean blocked      // nullable = don't change
) {}
