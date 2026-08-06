package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateUserRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 100)
        String password,

        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @NotBlank @Pattern(regexp = "^[0-9]{8}[A-Z]$|^[XYZ][0-9]{7}[A-Z]$",
                           message = "Formato de DNI/NIE inválido")
        String dni,

        @NotBlank @Size(max = 20)
        String phone,

        @NotBlank @Size(max = 100)
        String city,

        @Size(max = 100)
        String province,

        @NotBlank @Size(max = 100)
        String nationality,

        LocalDate birthDate
) {}
