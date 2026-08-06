package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(

        @NotBlank(message = "El título no puede estar vacío")
        @Size(max = 300, message = "El título no puede superar los 300 caracteres")
        String title,

        String description,

        String fileBase64,   // null = keep existing file
        String fileName,     // null = keep existing file name

        @NotNull
        boolean published
) {}
