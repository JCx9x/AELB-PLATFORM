package es.aelb.backendweb.infrastructure.web.dto.request;
import jakarta.validation.constraints.Email; import jakarta.validation.constraints.NotBlank;
/** {@code useRecoveryEmail}: el email indicado es el correo secundario de recuperación, no el principal. */
public record ForgotPasswordRequest(@NotBlank @Email String email, boolean useRecoveryEmail) {}
