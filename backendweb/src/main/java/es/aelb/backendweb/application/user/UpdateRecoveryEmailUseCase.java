package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;

/**
 * Añade, cambia o quita el correo secundario de recuperación del propio usuario.
 * Un correo nuevo (o uno aún sin verificar que se vuelve a enviar) dispara el
 * correo de verificación; hasta verificarlo no sirve para recuperar la contraseña.
 */
public class UpdateRecoveryEmailUseCase {

    private final UserRepository userRepository;
    private final EmailService   emailService;

    public UpdateRecoveryEmailUseCase(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService   = emailService;
    }

    public User execute(String userId, String recoveryEmail) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new User.NotFoundException(userId));

        if (recoveryEmail == null || recoveryEmail.isBlank()) {
            user.removeRecoveryEmail();
        } else {
            user.changeRecoveryEmail(Email.of(recoveryEmail));
        }
        userRepository.save(user);

        if (user.hasPendingRecoveryEmail()) {
            emailService.requestRecoveryEmailVerification(user);
        }
        return user;
    }
}
