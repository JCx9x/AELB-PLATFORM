package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.UserRole;
import es.aelb.backendweb.domain.user.valueobject.Email;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Una cuenta recién creada no verifica su email hasta hacer clic en el enlace enviado por
 * correo — hasta entonces, {@link LoginUseCase} debe negar el acceso aunque la contraseña
 * sea correcta y la cuenta no esté bloqueada. */
class LoginUseCaseTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final LoginUseCase.PasswordMatcher passwordMatcher = mock(LoginUseCase.PasswordMatcher.class);
    private final LoginUseCase loginUseCase = new LoginUseCase(userRepository, passwordMatcher);

    private User userWith(boolean blocked, boolean emailVerified) {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("hash");
        when(user.isActive()).thenReturn(!blocked);
        when(user.isEmailVerified()).thenReturn(emailVerified);
        return user;
    }

    private void stubExistingUser(User user) {
        when(userRepository.findByEmail(Email.of("user@example.com"))).thenReturn(Optional.of(user));
        when(passwordMatcher.matches("correct-password", "hash")).thenReturn(true);
    }

    @Test
    void credencialesCorrectasConEmailVerificadoYCuentaActivaPermitenElLogin() {
        User user = userWith(false, true);
        when(user.getId()).thenReturn(es.aelb.backendweb.domain.user.valueobject.UserId.of("user-1"));
        when(user.getEmail()).thenReturn(Email.of("user@example.com"));
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getFirstName()).thenReturn("Ana");
        when(user.getLastName()).thenReturn("Pérez");
        when(user.getTokenVersion()).thenReturn(0);
        stubExistingUser(user);

        LoginResult result = loginUseCase.execute(new LoginCommand("user@example.com", "correct-password"));

        assertEquals("user-1", result.userId());
    }

    @Test
    void emailSinVerificarBloqueaElAccesoAunqueLaContraseñaSeaCorrecta() {
        User user = userWith(false, false);
        stubExistingUser(user);

        assertThrows(LoginUseCase.EmailNotVerifiedException.class,
                () -> loginUseCase.execute(new LoginCommand("user@example.com", "correct-password")));
    }

    @Test
    void cuentaBloqueadaSePrioritizaSobreElEmailSinVerificar() {
        // Si ambas condiciones fallan, el mensaje de "bloqueado" es el más específico
        // (requiere intervención de un admin, no solo reenviar un email).
        User user = userWith(true, false);
        stubExistingUser(user);

        assertThrows(LoginUseCase.UserBlockedException.class,
                () -> loginUseCase.execute(new LoginCommand("user@example.com", "correct-password")));
    }

    @Test
    void contraseñaIncorrectaNoRevelaSiElEmailEstaVerificado() {
        User user = userWith(false, false);
        when(userRepository.findByEmail(Email.of("user@example.com"))).thenReturn(Optional.of(user));
        when(passwordMatcher.matches("wrong-password", "hash")).thenReturn(false);

        assertThrows(LoginUseCase.InvalidCredentialsException.class,
                () -> loginUseCase.execute(new LoginCommand("user@example.com", "wrong-password")));
    }

    @Test
    void emailInexistenteDaCredencialesIncorrectas() {
        when(userRepository.findByEmail(Email.of("ghost@example.com"))).thenReturn(Optional.empty());

        assertThrows(LoginUseCase.InvalidCredentialsException.class,
                () -> loginUseCase.execute(new LoginCommand("ghost@example.com", "whatever")));
    }
}
