package es.aelb.backendweb.application.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generación y hash del valor en claro de un refresh token.
 * Solo el hash SHA-256 se persiste; el valor en claro únicamente
 * existe en la cookie del cliente y en la respuesta inmediata al emitirlo.
 */
final class RefreshTokenCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RefreshTokenCrypto() {}

    static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
