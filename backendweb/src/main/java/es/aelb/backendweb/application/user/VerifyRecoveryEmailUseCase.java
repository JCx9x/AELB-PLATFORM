package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.email.EmailServiceAdapter;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RecoveryEmailVerificationTokenJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringRecoveryEmailVerificationTokenJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public class VerifyRecoveryEmailUseCase {

    private final SpringRecoveryEmailVerificationTokenJpaRepository tokens;
    private final UserRepository users;

    public VerifyRecoveryEmailUseCase(SpringRecoveryEmailVerificationTokenJpaRepository tokens, UserRepository users) {
        this.tokens = tokens;
        this.users  = users;
    }

    @Transactional
    public void execute(String token) {
        if (token == null || token.isBlank()) throw new VerifyEmailUseCase.InvalidVerificationTokenException();
        RecoveryEmailVerificationTokenJpaEntity t = tokens.findByTokenHash(EmailServiceAdapter.hash(token))
                .orElseThrow(VerifyEmailUseCase.InvalidVerificationTokenException::new);
        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VerifyEmailUseCase.InvalidVerificationTokenException();
        }
        User user = users.findById(UserId.of(t.getUserId()))
                .orElseThrow(VerifyEmailUseCase.InvalidVerificationTokenException::new);
        // Si el usuario cambió el correo de recuperación después de pedir este enlace, ya no vale.
        if (!user.verifyRecoveryEmail(Email.of(t.getRecoveryEmail()))) {
            throw new VerifyEmailUseCase.InvalidVerificationTokenException();
        }
        users.save(user);
        t.setUsed(true);
        tokens.save(t);
    }
}
