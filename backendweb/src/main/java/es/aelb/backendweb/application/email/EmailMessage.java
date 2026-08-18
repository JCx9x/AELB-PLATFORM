package es.aelb.backendweb.application.email;

public record EmailMessage(String recipient, String subject, String plainText, String html) {}
