package es.aelb.backendweb.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    @Test
    void acceptsASecretWithAtLeast256Bits() {
        assertDoesNotThrow(() -> JwtTokenProvider.validateSecret(
                "test-only-secret-with-at-least-32-bytes"));
    }

    @Test
    void rejectsMissingSecret() {
        assertThrows(IllegalStateException.class, () -> JwtTokenProvider.validateSecret(""));
    }

    @Test
    void rejectsKnownInsecureSecret() {
        assertThrows(IllegalStateException.class, () -> JwtTokenProvider.validateSecret(
                "insecure_default_change_me_in_production"));
    }

    @Test
    void rejectsSecretShorterThan256Bits() {
        assertThrows(IllegalStateException.class, () -> JwtTokenProvider.validateSecret("too-short"));
    }
}
