package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UpdateRecoveryEmailUseCaseTest {

    private final UserRepository users = mock(UserRepository.class);
    private final EmailService emails = mock(EmailService.class);
    private final UpdateRecoveryEmailUseCase useCase = new UpdateRecoveryEmailUseCase(users, emails);

    private User stored() {
        User user = User.create(Email.of("ana@example.com"), "hash", "Ana", "García",
                Dni.of("12345678Z"), null, null, null, null, LocalDate.of(1995, 5, 20));
        when(users.findById(any(UserId.class))).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void guardaElCorreoYEnviaLaVerificacion() {
        User user = stored();

        useCase.execute(user.getId().value(), "  Backup@Example.com ");

        assertThat(user.getRecoveryEmail()).isEqualTo(Email.of("backup@example.com"));
        verify(users).save(user);
        verify(emails).requestRecoveryEmailVerification(user);
    }

    @Test
    void vacioLoQuitaSinEnviarNada() {
        User user = stored();
        user.changeRecoveryEmail(Email.of("backup@example.com"));

        useCase.execute(user.getId().value(), "");

        assertThat(user.getRecoveryEmail()).isNull();
        verify(users).save(user);
        verifyNoInteractions(emails);
    }

    @Test
    void siYaEstaVerificadoNoReenvia() {
        User user = stored();
        user.changeRecoveryEmail(Email.of("backup@example.com"));
        user.verifyRecoveryEmail(Email.of("backup@example.com"));

        useCase.execute(user.getId().value(), "backup@example.com");

        verifyNoInteractions(emails);
    }
}
