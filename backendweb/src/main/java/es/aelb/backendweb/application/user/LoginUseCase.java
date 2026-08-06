package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Email;

/**
 * Caso de uso: autenticación de usuario.
 *
 * Orquesta:
 * 1. Búsqueda del usuario por email (fallo genérico si no existe).
 * 2. Verificación de la contraseña (fallo genérico si no coincide).
 * 3. Comprobación de que el usuario no está bloqueado.
 * 4. Devolución del resultado con los datos necesarios para emitir el token.
 *
 * El mensaje de error es intencionalmente genérico para no revelar
 * si el email existe o no en el sistema.
 */
public class LoginUseCase implements UseCase<LoginCommand, LoginResult> {

    private final UserRepository    userRepository;
    private final PasswordMatcher   passwordMatcher;

    public LoginUseCase(UserRepository userRepository, PasswordMatcher passwordMatcher) {
        this.userRepository  = userRepository;
        this.passwordMatcher = passwordMatcher;
    }

    @Override
    public LoginResult execute(LoginCommand cmd) {
        Email email = Email.of(cmd.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordMatcher.matches(cmd.rawPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new UserBlockedException();
        }

        return new LoginResult(
                user.getId().value(),
                user.getEmail().value(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName(),
                user.getTokenVersion()
        );
    }

    /** Puerto de salida para verificación de contraseñas. */
    public interface PasswordMatcher {
        boolean matches(String rawPassword, String encodedPassword);
    }

    public static final class InvalidCredentialsException extends DomainException {
        public InvalidCredentialsException() {
            super("Credenciales incorrectas");
        }
    }

    public static final class UserBlockedException extends DomainException {
        public UserBlockedException() {
            super("La cuenta está bloqueada. Contacta con administración.");
        }
    }
}
