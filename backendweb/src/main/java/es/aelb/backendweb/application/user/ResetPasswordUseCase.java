package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.email.EmailServiceAdapter;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.PasswordResetTokenJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringPasswordResetTokenJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public class ResetPasswordUseCase {
    public interface PasswordEncoder { String encode(String raw); }
    private final SpringPasswordResetTokenJpaRepository tokens; private final UserRepository users; private final PasswordEncoder encoder;
    public ResetPasswordUseCase(SpringPasswordResetTokenJpaRepository tokens, UserRepository users, PasswordEncoder encoder) { this.tokens=tokens;this.users=users;this.encoder=encoder; }
    @Transactional public void execute(String token, String newPassword) {
        if (token == null || token.isBlank()) throw new InvalidResetTokenException();
        PasswordResetTokenJpaEntity t=tokens.findByTokenHash(EmailServiceAdapter.hash(token)).orElseThrow(InvalidResetTokenException::new);
        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) throw new InvalidResetTokenException();
        User user=users.findById(UserId.of(t.getUserId())).orElseThrow(InvalidResetTokenException::new);
        user.changePassword(encoder.encode(newPassword)); users.save(user); t.setUsed(true); tokens.save(t);
    }
    public static final class InvalidResetTokenException extends RuntimeException { public InvalidResetTokenException(){super("El enlace de recuperación no es válido o ha expirado");} }
}
