package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Autoedición de perfil. No incluye birthDate a propósito: el usuario no
 * puede modificar su fecha de nacimiento una vez registrado, solo GESTOR/ADMIN
 * (ver {@link ManagementUpdateUserRequest}).
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @Size(max = 20)
        String phone
) {}
