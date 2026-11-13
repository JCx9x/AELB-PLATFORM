package es.aelb.backendweb.domain.user.valueobject;

import es.aelb.backendweb.domain.shared.exception.DomainException;

import java.util.ArrayList;
import java.util.List;

/**
 * Requisitos mínimos de una contraseña nueva (registro, cambio y recuperación).
 * Las contraseñas ya guardadas no se revalidan: solo aplica al fijar una nueva.
 * El frontend replica estas reglas en app/lib/passwordPolicy.ts.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 100;

    private PasswordPolicy() {}

    public static void validate(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword;
        List<String> missing = new ArrayList<>();
        if (password.length() < MIN_LENGTH)                 missing.add("al menos " + MIN_LENGTH + " caracteres");
        if (password.chars().noneMatch(Character::isLowerCase)) missing.add("una letra minúscula");
        if (password.chars().noneMatch(Character::isUpperCase)) missing.add("una letra mayúscula");
        if (password.chars().noneMatch(Character::isDigit))     missing.add("un número");

        if (!missing.isEmpty()) {
            throw new WeakPasswordException("La contraseña debe tener " + String.join(", ", missing) + ".");
        }
        if (password.length() > MAX_LENGTH) {
            throw new WeakPasswordException("La contraseña no puede superar los " + MAX_LENGTH + " caracteres.");
        }
    }

    public static final class WeakPasswordException extends DomainException {
        public WeakPasswordException(String message) {
            super(message);
        }
    }
}
