package es.aelb.backendweb.infrastructure.web.dto.request;
import jakarta.validation.constraints.NotBlank;
public record VerificationTokenRequest(@NotBlank String token) {}
