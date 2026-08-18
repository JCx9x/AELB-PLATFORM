package es.aelb.backendweb.infrastructure.email;
public class TemporaryEmailException extends RuntimeException { public TemporaryEmailException(String message, Throwable cause){super(message,cause);} public TemporaryEmailException(String message){super(message);} }
