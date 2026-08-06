package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.UserRole;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.LocalDate;

public class AdminUpdateUserUseCase {

    private final UserRepository userRepository;

    public AdminUpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(AdminUpdateUserCommand cmd) {
        User user = userRepository.findById(UserId.of(cmd.userId()))
                .orElseThrow(() -> new User.NotFoundException(cmd.userId()));

        // Update profile fields
        LocalDate birthDate = (cmd.birthDate() != null && !cmd.birthDate().isBlank())
                ? LocalDate.parse(cmd.birthDate())
                : null;
        user.updateProfile(cmd.firstName(), cmd.lastName(), cmd.phone(), birthDate);

        // Update role if provided
        if (cmd.role() != null && !cmd.role().isBlank()) {
            user.changeRole(UserRole.valueOf(cmd.role().toUpperCase()));
        }

        // Update blocked status if provided
        if (cmd.blocked() != null) {
            user.setBlocked(cmd.blocked());
        }

        userRepository.save(user);
        return user;
    }
}
