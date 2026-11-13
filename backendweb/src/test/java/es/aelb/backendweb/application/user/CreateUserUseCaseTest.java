package es.aelb.backendweb.application.user;

import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateUserUseCaseTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CreateUserUseCase.PasswordEncoder encoder = raw -> "hash:" + raw;
    private final CreateUserUseCase useCase = new CreateUserUseCase(userRepository, encoder);

    private CreateUserCommand command(String password) {
        return new CreateUserCommand("nuevo@example.com", password, "Ana", "García", "12345678Z",
                "+34600000000", "Madrid", null, "ES", LocalDate.of(1995, 5, 20));
    }

    @Test
    void contrasenaDebilNoCreaLaCuenta() {
        assertThrows(PasswordPolicy.WeakPasswordException.class, () -> useCase.execute(command("12345678")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void contrasenaQueCumpleLaPoliticaCreaLaCuenta() {
        String id = useCase.execute(command("Lucha2026"));

        assertNotNull(id);
        verify(userRepository).save(any());
    }
}
