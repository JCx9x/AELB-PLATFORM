package es.aelb.backendweb.application.user;
import es.aelb.backendweb.domain.user.User; import es.aelb.backendweb.domain.user.UserRepository; import es.aelb.backendweb.domain.user.valueobject.UserId; import es.aelb.backendweb.infrastructure.email.EmailServiceAdapter; import es.aelb.backendweb.infrastructure.persistence.jpa.entity.EmailVerificationTokenJpaEntity; import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringEmailVerificationTokenJpaRepository; import org.springframework.transaction.annotation.Transactional; import java.time.LocalDateTime;
public class VerifyEmailUseCase {
 private final SpringEmailVerificationTokenJpaRepository tokens; private final UserRepository users;
 public VerifyEmailUseCase(SpringEmailVerificationTokenJpaRepository tokens,UserRepository users){this.tokens=tokens;this.users=users;}
 @Transactional public void execute(String token){if(token==null||token.isBlank())throw new InvalidVerificationTokenException(); EmailVerificationTokenJpaEntity t=tokens.findByTokenHash(EmailServiceAdapter.hash(token)).orElseThrow(InvalidVerificationTokenException::new); if(t.isUsed()||t.getExpiresAt().isBefore(LocalDateTime.now()))throw new InvalidVerificationTokenException(); User u=users.findById(UserId.of(t.getUserId())).orElseThrow(InvalidVerificationTokenException::new);u.verifyEmail();users.save(u);t.setUsed(true);tokens.save(t);}
 public static final class InvalidVerificationTokenException extends RuntimeException{public InvalidVerificationTokenException(){super("El enlace de verificación no es válido o ha expirado");}}
}
