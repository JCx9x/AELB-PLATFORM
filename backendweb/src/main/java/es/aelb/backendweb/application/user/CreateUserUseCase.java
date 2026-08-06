package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.shared.exception.DomainException;

/**
 * Caso de uso: registro de nuevo usuario.
 *
 * Orquesta:
 * 1. Validación de unicidad de email y DNI.
 * 2. Hash de la contraseña (delegado al puerto PasswordEncoder).
 * 3. Construcción del agregado User.
 * 4. Persistencia.
 */
public class CreateUserUseCase implements UseCase<CreateUserCommand, String> {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;

    public CreateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String execute(CreateUserCommand cmd) {
        Email email = Email.of(cmd.email());
        Dni   dni   = Dni.of(cmd.dni());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email.value());
        }
        if (userRepository.existsByDni(dni)) {
            throw new DniAlreadyExistsException(dni.value());
        }

        User user = User.create(
                email,
                passwordEncoder.encode(cmd.rawPassword()),
                cmd.firstName(),
                cmd.lastName(),
                dni,
                cmd.phone(),
                cmd.city(),
                cmd.province(),
                cmd.nationality(),
                cmd.birthDate()
        );

        userRepository.save(user);
        return user.getId().value();
    }

    /** Puerto de salida secundario para cifrado de contraseñas. */
    public interface PasswordEncoder {
        String encode(String rawPassword);
    }

    public static final class EmailAlreadyExistsException extends DomainException {
        public EmailAlreadyExistsException(String email) {
            super("Ya existe un usuario con el email: " + email);
        }
    }

    public static final class DniAlreadyExistsException extends DomainException {
        public DniAlreadyExistsException(String dni) {
            super("Ya existe un usuario con el DNI: " + dni);
        }
    }
}
