package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.email.EmailServiceAdapter;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RecoveryEmailVerificationTokenJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringRecoveryEmailVerificationTokenJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerifyRecoveryEmailUseCaseTest {

    private final SpringRecoveryEmailVerificationTokenJpaRepository tokens = mock(SpringRecoveryEmailVerificationTokenJpaRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final VerifyRecoveryEmailUseCase useCase = new VerifyRecoveryEmailUseCase(tokens, users);

    private User userWithPending(String recoveryEmail) {
        User user = User.create(Email.of("ana@example.com"), "hash", "Ana", "García",
                Dni.of("12345678Z"), null, null, null, null, LocalDate.of(1995, 5, 20));
        user.changeRecoveryEmail(Email.of(recoveryEmail));
        when(users.findById(any(UserId.class))).thenReturn(Optional.of(user));
        return user;
    }

    private RecoveryEmailVerificationTokenJpaEntity token(String recoveryEmail, LocalDateTime expiresAt) {
        RecoveryEmailVerificationTokenJpaEntity t = new RecoveryEmailVerificationTokenJpaEntity();
        t.setUserId("u1"); t.setRecoveryEmail(recoveryEmail); t.setTokenHash(EmailServiceAdapter.hash("tok"));
        t.setExpiresAt(expiresAt); t.setCreatedAt(LocalDateTime.now()); t.setUsed(false);
        when(tokens.findByTokenHash(EmailServiceAdapter.hash("tok"))).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void verificaElCorreoYConsumeElToken() {
        User user = userWithPending("backup@example.com");
        RecoveryEmailVerificationTokenJpaEntity t = token("backup@example.com", LocalDateTime.now().plusHours(1));

        useCase.execute("tok");

        assertThat(user.isRecoveryEmailVerified()).isTrue();
        assertThat(t.isUsed()).isTrue();
        verify(users).save(user);
    }

    @Test
    void rechazaElEnlaceSiElUsuarioCambioElCorreoDespues() {
        User user = userWithPending("nuevo@example.com");
        token("viejo@example.com", LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> useCase.execute("tok"))
                .isInstanceOf(VerifyEmailUseCase.InvalidVerificationTokenException.class);
        assertThat(user.isRecoveryEmailVerified()).isFalse();
    }

    @Test
    void rechazaUnTokenCaducado() {
        userWithPending("backup@example.com");
        token("backup@example.com", LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> useCase.execute("tok"))
                .isInstanceOf(VerifyEmailUseCase.InvalidVerificationTokenException.class);
    }
}
