package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Gestión de perfil por GESTOR/ADMIN. A diferencia de {@link UpdateProfileRequest},
 * incluye birthDate: es la única vía (junto con {@link AdminUpdateUserRequest})
 * para cambiar la fecha de nacimiento tras el registro.
 */
public record ManagementUpdateUserRequest(
        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @Size(max = 20)
        String phone,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento no puede ser futura")
        LocalDate birthDate
) {}
