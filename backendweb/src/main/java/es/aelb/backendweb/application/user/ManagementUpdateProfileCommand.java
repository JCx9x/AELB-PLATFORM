package es.aelb.backendweb.application.user;

import java.time.LocalDate;

/** Edición de perfil por GESTOR/ADMIN. Es la única vía, junto con {@link AdminUpdateUserCommand}, para cambiar birthDate. */
public record ManagementUpdateProfileCommand(
        String    userId,
        String    firstName,
        String    lastName,
        String    phone,
        LocalDate birthDate
) {}
