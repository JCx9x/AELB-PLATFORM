package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePaymentStatusRequest(
        @NotBlank
        @Pattern(regexp = "PENDING|PAID", message = "El estado debe ser PENDING o PAID")
        String status
) {}
