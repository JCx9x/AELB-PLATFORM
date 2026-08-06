package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

public class DeleteUserUseCase {

    private final UserRepository userRepository;

    public DeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(String userId, String requestingUserId) {
        if (userId.equals(requestingUserId)) {
            throw new IllegalArgumentException("No puedes eliminar tu propia cuenta");
        }
        userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new User.NotFoundException(userId));
        userRepository.delete(UserId.of(userId));
    }
}
