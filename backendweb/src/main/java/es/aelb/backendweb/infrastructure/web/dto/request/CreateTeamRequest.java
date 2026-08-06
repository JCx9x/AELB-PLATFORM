package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String responsibleName,
        @NotBlank @Size(max = 100) String responsibleLastName,
        @Size(max = 20)            String phone,
        @NotBlank @Size(max = 100) String province,
        @NotBlank @Size(max = 100) String locality,
                                   String logoBase64,
        @Size(max = 50)            String logoType
) {}
