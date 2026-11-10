package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Email;

/**
 * Variante de {@link ResendEmailVerificationUseCase} para un usuario que todavía no puede
 * autenticarse (es justamente el motivo por el que necesita este reenvío): identifica la
 * cuenta por email en vez de por el principal de una sesión ya autenticada. Mismo criterio
 * que {@link RequestPasswordResetUseCase}: si el email no existe, no hace nada — la
 * respuesta del controlador es idéntica en ambos casos para no delatar qué cuentas existen.
 */
public class ResendEmailVerificationByEmailUseCase {

    private final UserRepository userRepository;
    private final EmailService   emailService;

    public ResendEmailVerificationByEmailUseCase(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService   = emailService;
    }

    public void execute(String email) {
        if (email == null || email.isBlank()) return;
        userRepository.findByEmail(Email.of(email.trim().toLowerCase()))
                .ifPresent(emailService::requestEmailVerification);
    }
}
