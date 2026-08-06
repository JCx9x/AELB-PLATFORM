package es.aelb.backendweb.domain.user.valueobject;

import es.aelb.backendweb.domain.shared.exception.DomainException;

import java.util.regex.Pattern;

/**
 * DNI/NIE español. El formato se valida pero la letra control
 * se comprueba para garantizar integridad de datos.
 */
public record Dni(String value) {

    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}[A-Z]$");
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZ][0-9]{7}[A-Z]$");
    private static final String  LETTERS     = "TRWAGMYFPDXBNJZSQVHLCKE";

    public Dni {
        if (value == null || value.isBlank()) {
            throw new InvalidDniException("El DNI no puede estar vacío");
        }
        value = value.trim().toUpperCase();

        if (!DNI_PATTERN.matcher(value).matches() && !NIE_PATTERN.matcher(value).matches()) {
            throw new InvalidDniException("Formato de DNI/NIE inválido: " + value);
        }
        if (!hasValidControlLetter(value)) {
            throw new InvalidDniException("Letra de control incorrecta: " + value);
        }
    }

    public static Dni of(String value) {
        return new Dni(value);
    }

    private static boolean hasValidControlLetter(String dni) {
        String normalized = dni.startsWith("X") ? dni.replace('X', '0')
                : dni.startsWith("Y") ? dni.replace('Y', '1')
                : dni.startsWith("Z") ? dni.replace('Z', '2')
                : dni;

        int number = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
        char expected = LETTERS.charAt(number % 23);
        return expected == dni.charAt(dni.length() - 1);
    }

    @Override
    public String toString() {
        return value;
    }

    public static final class InvalidDniException extends DomainException {
        public InvalidDniException(String message) {
            super(message);
        }
    }
}
