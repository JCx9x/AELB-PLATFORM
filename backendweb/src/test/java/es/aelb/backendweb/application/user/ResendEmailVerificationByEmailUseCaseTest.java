package es.aelb.backendweb.application.user;

import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Email;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/** Vía de reenvío para quien no puede autenticarse para usar {@link ResendEmailVerificationUseCase}. */
class ResendEmailVerificationByEmailUseCaseTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final ResendEmailVerificationByEmailUseCase useCase =
            new ResendEmailVerificationByEmailUseCase(userRepository, emailService);

    @Test
    void siLaCuentaExisteDelegaElReenvioAlEmailService() {
        User user = mock(User.class);
        when(userRepository.findByEmail(Email.of("user@example.com"))).thenReturn(Optional.of(user));

        useCase.execute("user@example.com");

        verify(emailService).requestEmailVerification(user);
    }

    @Test
    void siLaCuentaNoExisteNoHaceNadaYNoRevelaNada() {
        when(userRepository.findByEmail(Email.of("ghost@example.com"))).thenReturn(Optional.empty());

        useCase.execute("ghost@example.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void emailVacioONuloNoLlamaAlRepositorio() {
        useCase.execute("");
        useCase.execute(null);

        verifyNoInteractions(userRepository, emailService);
    }

    @Test
    void normalizaMayusculasYEspaciosAntesDeBuscar() {
        User user = mock(User.class);
        when(userRepository.findByEmail(Email.of("user@example.com"))).thenReturn(Optional.of(user));

        useCase.execute("  USER@example.com  ");

        verify(emailService).requestEmailVerification(user);
    }
}
