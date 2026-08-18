package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.email.EmailService;

public class RequestPasswordResetUseCase {
    private final EmailService emailService;
    public RequestPasswordResetUseCase(EmailService emailService) { this.emailService = emailService; }
    public void execute(String email) { emailService.requestPasswordReset(email == null ? "" : email.trim().toLowerCase()); }
}
