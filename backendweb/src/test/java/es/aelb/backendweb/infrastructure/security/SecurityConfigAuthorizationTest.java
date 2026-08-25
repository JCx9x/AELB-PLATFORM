package es.aelb.backendweb.infrastructure.security;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "aelb.jwt.secret=test-only-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
class SecurityConfigAuthorizationTest {

    private static final String DNI_CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthCookieProperties authCookieProperties;

    @Test
    void registrationPdfRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/championships/00000000-0000-0000-0000-000000000000/registrations/pdf"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * @WithMockUser doesn't work here: with SessionCreationPolicy.STATELESS,
     * Spring Security's SecurityContextHolderFilter reloads the (empty)
     * context from the configured repository at the start of every request,
     * wiping whatever @WithMockUser placed in SecurityContextHolder before
     * the filter chain ran. So this authenticates the same way the real
     * frontend does: a real JWT for a real, persisted GESTOR user, sent as
     * the actual auth cookie — exercising JwtAuthenticationFilter for real
     * instead of bypassing it.
     */
    @Test
    void gestorCannotChangeRoleOrBlockedStatus() throws Exception {
        User gestor = persistGestor();
        String token = jwtTokenProvider.generateToken(
                gestor.getId().value(), gestor.getEmail().value(), "GESTOR", gestor.getTokenVersion());

        mockMvc.perform(put("/api/users/00000000-0000-0000-0000-000000000000/admin")
                        .with(csrf())
                        .cookie(new Cookie(authCookieProperties.getName(), token))
                        .contentType("application/json")
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"role\":\"ADMIN\",\"blocked\":true}"))
                .andExpect(status().isForbidden());
    }

    private User persistGestor() {
        int number = (int) (System.nanoTime() % 100_000_000L);
        String dni = "%08d".formatted(number) + DNI_CONTROL_LETTERS.charAt(number % 23);
        User user = User.create(
                Email.of("gestor-" + number + "@example.test"),
                "irrelevant-hash",
                "Test",
                "Gestor",
                Dni.of(dni),
                "600000000",
                "Madrid",
                "Madrid",
                "ES",
                LocalDate.of(1990, 1, 1)
        );
        user.promoteToGestor();
        userRepository.save(user);
        return user;
    }
}
