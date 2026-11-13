package es.aelb.backendweb.domain.user;

import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class UserRecoveryEmailTest {

    private User newUser() {
        return User.create(Email.of("ana@example.com"), "hash", "Ana", "García",
                Dni.of("12345678Z"), null, null, null, null, LocalDate.of(1995, 5, 20));
    }

    @Test
    void unCorreoNuevoQuedaPendienteDeVerificar() {
        User user = newUser();
        user.changeRecoveryEmail(Email.of("ana.backup@example.com"));

        assertThat(user.getRecoveryEmail()).isEqualTo(Email.of("ana.backup@example.com"));
        assertThat(user.isRecoveryEmailVerified()).isFalse();
        assertThat(user.hasPendingRecoveryEmail()).isTrue();
    }

    @Test
    void noPuedeSerIgualAlCorreoPrincipal() {
        User user = newUser();
        assertThatThrownBy(() -> user.changeRecoveryEmail(Email.of("ANA@example.com")))
                .isInstanceOf(User.RecoveryEmailSameAsPrimaryException.class);
    }

    @Test
    void soloSeVerificaSiSigueSiendoLaMismaDireccion() {
        User user = newUser();
        user.changeRecoveryEmail(Email.of("vieja@example.com"));
        user.changeRecoveryEmail(Email.of("nueva@example.com"));

        assertThat(user.verifyRecoveryEmail(Email.of("vieja@example.com"))).isFalse();
        assertThat(user.isRecoveryEmailVerified()).isFalse();

        assertThat(user.verifyRecoveryEmail(Email.of("nueva@example.com"))).isTrue();
        assertThat(user.isRecoveryEmailVerified()).isTrue();
    }

    @Test
    void cambiarloTrasVerificarloVuelveAPedirVerificacion() {
        User user = newUser();
        user.changeRecoveryEmail(Email.of("uno@example.com"));
        user.verifyRecoveryEmail(Email.of("uno@example.com"));

        user.changeRecoveryEmail(Email.of("uno@example.com"));
        assertThat(user.isRecoveryEmailVerified()).as("guardar el mismo no lo desverifica").isTrue();

        user.changeRecoveryEmail(Email.of("dos@example.com"));
        assertThat(user.isRecoveryEmailVerified()).isFalse();
    }

    @Test
    void quitarloLoBorra() {
        User user = newUser();
        user.changeRecoveryEmail(Email.of("uno@example.com"));
        user.verifyRecoveryEmail(Email.of("uno@example.com"));

        user.removeRecoveryEmail();

        assertThat(user.getRecoveryEmail()).isNull();
        assertThat(user.isRecoveryEmailVerified()).isFalse();
    }
}
