package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

public class ChangePasswordUseCase {

    public interface PasswordMatcher {
        boolean matches(String rawPassword, String encodedPassword);
    }

    public interface PasswordEncoder {
        String encode(String rawPassword);
    }

    private final UserRepository  userRepository;
    private final PasswordMatcher passwordMatcher;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordUseCase(
            UserRepository userRepository,
            PasswordMatcher passwordMatcher,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository  = userRepository;
        this.passwordMatcher = passwordMatcher;
        this.passwordEncoder = passwordEncoder;
    }

    public void execute(ChangePasswordCommand cmd) {
        User user = userRepository.findById(UserId.of(cmd.userId()))
                .orElseThrow(() -> new UserNotFoundException(cmd.userId()));

        if (!passwordMatcher.matches(cmd.currentPassword(), user.getPasswordHash())) {
            throw new WrongCurrentPasswordException();
        }

        user.changePassword(passwordEncoder.encode(cmd.newPassword()));
        userRepository.save(user);
    }

    public static final class UserNotFoundException extends DomainException {
        public UserNotFoundException(String userId) {
            super("Usuario no encontrado: " + userId);
        }
    }

    public static final class WrongCurrentPasswordException extends DomainException {
        public WrongCurrentPasswordException() {
            super("La contraseña actual es incorrecta");
        }
    }
}
