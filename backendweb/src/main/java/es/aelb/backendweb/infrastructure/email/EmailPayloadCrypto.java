package es.aelb.backendweb.infrastructure.email;

import es.aelb.backendweb.infrastructure.config.EmailProperties;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EmailPayloadCrypto {
    private final EmailProperties properties; private final SecureRandom random = new SecureRandom();
    public EmailPayloadCrypto(EmailProperties properties) { this.properties = properties; }
    public String encrypt(String value) {
        try { Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); byte[] iv = new byte[12]; random.nextBytes(iv); c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv)); byte[] encrypted = c.doFinal(value.getBytes(StandardCharsets.UTF_8)); byte[] all = new byte[iv.length + encrypted.length]; System.arraycopy(iv,0,all,0,iv.length); System.arraycopy(encrypted,0,all,iv.length,encrypted.length); return Base64.getEncoder().encodeToString(all); }
        catch (Exception e) { throw new IllegalStateException("No se pudo proteger el payload del email", e); }
    }
    public String decrypt(String value) {
        try { byte[] all = Base64.getDecoder().decode(value); Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, all, 0, 12)); return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8); }
        catch (Exception e) { throw new IllegalStateException("No se pudo descifrar el payload del email", e); }
    }
    private SecretKeySpec key() { try { byte[] hash = MessageDigest.getInstance("SHA-256").digest(properties.getTokenEncryptionKey().getBytes(StandardCharsets.UTF_8)); return new SecretKeySpec(hash, "AES"); } catch (Exception e) { throw new IllegalStateException(e); } }
}
