package es.aelb.backendweb.application.email;

public interface EmailSender {
    void send(EmailMessage message);
}
