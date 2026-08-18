package es.aelb.backendweb.application.email;

import es.aelb.backendweb.domain.user.User;

public interface EmailService {
    EmailService NOOP = new EmailService() {
        public void requestEmailVerification(User user) {}
        public void requestPasswordReset(String email) {}
        public void requestRegistrationConfirmation(String recipient, String recipientName, String registrationId, String championshipName) {}
        public void requestPaymentConfirmation(String recipient, String recipientName, String paymentId, String description, String amount) {}
    };
    void requestEmailVerification(User user);
    void requestPasswordReset(String email);
    void requestRegistrationConfirmation(String recipient, String recipientName, String registrationId, String championshipName);
    void requestPaymentConfirmation(String recipient, String recipientName, String paymentId, String description, String amount);
}
