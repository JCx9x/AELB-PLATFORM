package es.aelb.backendweb.infrastructure.email;
public class PermanentEmailException extends RuntimeException { public PermanentEmailException(String message, Throwable cause){super(message,cause);} public PermanentEmailException(String message){super(message);} }
