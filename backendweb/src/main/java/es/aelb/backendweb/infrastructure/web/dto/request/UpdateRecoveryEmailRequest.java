package es.aelb.backendweb.infrastructure.web.dto.request;
import jakarta.validation.constraints.Email; import jakarta.validation.constraints.Size;
/** {@code recoveryEmail} vacío o null quita el correo de recuperación. */
public record UpdateRecoveryEmailRequest(@Email @Size(max = 255) String recoveryEmail) {}
