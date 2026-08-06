package es.aelb.backendweb.domain.user.valueobject;

import es.aelb.backendweb.domain.shared.exception.DomainException;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("El email no puede estar vacío");
        }
        value = value.trim().toLowerCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException("Formato de email inválido: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public static final class InvalidEmailException extends DomainException {
        public InvalidEmailException(String message) {
            super(message);
        }
    }
}
