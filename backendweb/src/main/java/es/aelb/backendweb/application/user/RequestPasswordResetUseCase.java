package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.email.EmailService;

public class RequestPasswordResetUseCase {
    private final EmailService emailService;
    public RequestPasswordResetUseCase(EmailService emailService) { this.emailService = emailService; }
    public void execute(String email) { emailService.requestPasswordReset(normalize(email)); }
    /** Para quien no recuerda su correo principal: el enlace se envía a su correo de recuperación verificado. */
    public void executeViaRecoveryEmail(String recoveryEmail) { emailService.requestPasswordResetViaRecoveryEmail(normalize(recoveryEmail)); }
    private static String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(); }
}
