package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResultRequest(

        @NotBlank(message = "El título no puede estar vacío")
        @Size(max = 300, message = "El título no puede superar los 300 caracteres")
        String title,

        String description,

        @NotBlank(message = "El PDF no puede estar vacío")
        String pdfBase64,

        @NotBlank(message = "El nombre del archivo no puede estar vacío")
        String pdfFileName,

        @NotNull
        boolean published
) {}
