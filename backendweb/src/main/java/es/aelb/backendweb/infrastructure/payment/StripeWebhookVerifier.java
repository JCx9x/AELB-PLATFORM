package es.aelb.backendweb.infrastructure.payment;

import es.aelb.backendweb.infrastructure.config.StripeProperties;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/** Verifica la firma Stripe-Signature sobre el cuerpo bruto del webhook. */
public class StripeWebhookVerifier {
    private static final long MAX_TIMESTAMP_AGE_SECONDS = 300;
    private final StripeProperties properties;
    public StripeWebhookVerifier(StripeProperties properties) { this.properties = properties; }

    public boolean isValid(String payload, String signatureHeader) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank() || signatureHeader == null) return false;
        String timestamp = null;
        for (String part : signatureHeader.split(",")) if (part.startsWith("t=")) timestamp = part.substring(2);
        if (timestamp == null) return false;
        try {
            long seconds = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - seconds) > MAX_TIMESTAMP_AGE_SECONDS) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            for (String part : signatureHeader.split(",")) {
                if (part.startsWith("v1=") && MessageDigest.isEqual(expected, hexToBytes(part.substring(3)))) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }
    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) return new byte[0];
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) data[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        return data;
    }
}
