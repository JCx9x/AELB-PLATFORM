package es.aelb.backendweb.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerAuthorizationTest {

    private static final String OWNER_ID = "owner-id";
    private static final String OTHER_ID = "other-id";

    @Test
    void userCanReadOnlyTheirOwnProfile() {
        var authentication = authentication(OWNER_ID, "ROLE_USER");

        assertTrue(UserController.canReadProfile(authentication, OWNER_ID));
        assertFalse(UserController.canReadProfile(authentication, OTHER_ID));
    }

    @Test
    void gestorCanReadAnyProfile() {
        assertTrue(UserController.canReadProfile(authentication(OWNER_ID, "ROLE_GESTOR"), OTHER_ID));
    }

    @Test
    void adminCanReadAnyProfile() {
        assertTrue(UserController.canReadProfile(authentication(OWNER_ID, "ROLE_ADMIN"), OTHER_ID));
    }

    private UsernamePasswordAuthenticationToken authentication(String userId, String role) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
