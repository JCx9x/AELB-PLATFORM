package es.aelb.backendweb.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "aelb.jwt.secret=test-only-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
class SecurityConfigAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationPdfRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/championships/00000000-0000-0000-0000-000000000000/registrations/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GESTOR")
    void gestorCannotChangeRoleOrBlockedStatus() throws Exception {
        mockMvc.perform(put("/api/users/00000000-0000-0000-0000-000000000000/admin")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"role\":\"ADMIN\",\"blocked\":true}"))
                .andExpect(status().isForbidden());
    }
}
