package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(

        @NotBlank(message = "El título no puede estar vacío")
        @Size(max = 300, message = "El título no puede superar los 300 caracteres")
        String title,

        String description,

        @NotBlank(message = "El archivo no puede estar vacío")
        @Size(max = 14000000, message = "El documento supera el tamaño máximo permitido")
        String fileBase64,

        @NotBlank(message = "El nombre del archivo no puede estar vacío")
        @Size(max = 255, message = "El nombre del archivo no puede superar los 255 caracteres")
        String fileName,

        @NotNull
        boolean published
) {}
