package es.aelb.backendweb.infrastructure.email;
import es.aelb.backendweb.infrastructure.config.EmailProperties; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class EmailRateLimiterTest {
 @Test void respetaElMaximoGlobalSinDormir(){EmailProperties p=new EmailProperties();p.setMaxPerSecond(2);EmailRateLimiter limiter=new EmailRateLimiter(p);assertTrue(limiter.tryAcquire());assertFalse(limiter.tryAcquire());}
}
