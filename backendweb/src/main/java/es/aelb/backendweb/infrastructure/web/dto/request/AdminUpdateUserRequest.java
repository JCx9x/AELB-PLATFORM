package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 20)            String phone,
                                   String birthDate,  // ISO date, nullable
                                   String role,       // nullable = don't change
                                   Boolean blocked    // nullable = don't change
) {}
