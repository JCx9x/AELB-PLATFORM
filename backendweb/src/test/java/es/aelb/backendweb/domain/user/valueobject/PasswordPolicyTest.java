package es.aelb.backendweb.domain.user.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "password", "PASSWORD1", "password1", "Abcdefgh", "Ab1"})
    void rechazaContrasenasQueNoCumplenLosRequisitos(String password) {
        assertThrows(PasswordPolicy.WeakPasswordException.class, () -> PasswordPolicy.validate(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abcdefg1", "Lucha2026", "Brazo de hierro 7"})
    void aceptaContrasenasConLongitudMayusculaMinusculaYNumero(String password) {
        assertDoesNotThrow(() -> PasswordPolicy.validate(password));
    }

    @Test
    void elMensajeIndicaTodoLoQueFalta() {
        var e = assertThrows(PasswordPolicy.WeakPasswordException.class, () -> PasswordPolicy.validate("12345678"));
        assertEquals("La contraseña debe tener una letra minúscula, una letra mayúscula.", e.getMessage());
    }

    @Test
    void contrasenaNulaSeRechaza() {
        assertThrows(PasswordPolicy.WeakPasswordException.class, () -> PasswordPolicy.validate(null));
    }

    @Test
    void rechazaContrasenasDemasiadoLargas() {
        assertThrows(PasswordPolicy.WeakPasswordException.class, () -> PasswordPolicy.validate("Aa1" + "x".repeat(98)));
    }
}
