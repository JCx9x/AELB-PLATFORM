package es.aelb.backendweb.infrastructure.email;
import es.aelb.backendweb.infrastructure.config.EmailProperties; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class EmailPayloadCryptoTest { @Test void cifraYDescifraPayload(){EmailProperties p=new EmailProperties();p.setTokenEncryptionKey("test-secret");EmailPayloadCrypto c=new EmailPayloadCrypto(p);String encrypted=c.encrypt("token-value");assertNotEquals("token-value",encrypted);assertEquals("token-value",c.decrypt(encrypted));} }
