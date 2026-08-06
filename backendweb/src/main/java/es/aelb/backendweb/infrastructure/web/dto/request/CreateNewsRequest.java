package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNewsRequest(
        @NotBlank @Size(max = 300)
        String  title,

        @NotBlank
        String  content,

        String  imageKey,

        boolean published
) {}
