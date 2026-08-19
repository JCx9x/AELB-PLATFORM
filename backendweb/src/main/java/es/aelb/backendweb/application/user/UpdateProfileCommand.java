package es.aelb.backendweb.application.user;

/** Autoedición de perfil. No incluye birthDate: el usuario no puede modificar su fecha de nacimiento. */
public record UpdateProfileCommand(
        String userId,
        String firstName,
        String lastName,
        String phone
) {}
