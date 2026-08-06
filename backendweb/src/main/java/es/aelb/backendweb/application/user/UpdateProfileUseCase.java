package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

public class UpdateProfileUseCase {

    private final UserRepository userRepository;

    public UpdateProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(UpdateProfileCommand cmd) {
        User user = userRepository.findById(UserId.of(cmd.userId()))
                .orElseThrow(() -> new UserNotFoundException(cmd.userId()));

        user.updateProfile(cmd.firstName(), cmd.lastName(), cmd.phone(), cmd.birthDate());
        userRepository.save(user);
        return user;
    }

    public static final class UserNotFoundException extends DomainException {
        public UserNotFoundException(String userId) {
            super("Usuario no encontrado: " + userId);
        }
    }
}
