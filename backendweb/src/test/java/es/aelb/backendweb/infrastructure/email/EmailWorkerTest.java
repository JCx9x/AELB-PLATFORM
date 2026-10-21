package es.aelb.backendweb.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.application.email.EmailSender;
import es.aelb.backendweb.infrastructure.config.EmailProperties;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.EmailOutboxJpaEntity;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;

class EmailWorkerTest {
    @Test void emailDeshabilitadoNoReclamaNiEnvía() {
        EmailProperties p = properties(); p.setEnabled(false); p.setWorkerEnabled(false);
        EmailSender sender = mock(EmailSender.class);
        EmailOutboxTransactionService tx = mock(EmailOutboxTransactionService.class);

        new EmailWorker(sender, new EmailPayloadCrypto(p), p, new ObjectMapper(), new EmailRateLimiter(p), tx, Runnable::run).tick();

        verify(tx, never()).claim();
        verifyNoInteractions(sender);
    }

    @Test void emailEnviadoPasaACompletado() throws Exception {
        EmailProperties p = properties();
        EmailPayloadCrypto crypto = new EmailPayloadCrypto(p);
        EmailOutboxJpaEntity job = job(crypto, p);
        EmailSender sender = mock(EmailSender.class);
        EmailOutboxTransactionService tx = mock(EmailOutboxTransactionService.class);
        when(tx.claim()).thenReturn(List.of(job));
        new EmailWorker(sender, crypto, p, new ObjectMapper(), new EmailRateLimiter(p), tx, Runnable::run).tick();
        verify(sender).send(any()); verify(tx).sent(job); verify(tx, never()).retry(any(), anyString(), anyInt(), any());
    }

    @Test void falloTemporalProgramaRetryYNoHaceLoopInfinito() {
        EmailProperties p = properties(); EmailPayloadCrypto crypto = new EmailPayloadCrypto(p); EmailOutboxJpaEntity job = job(crypto, p);
        EmailSender sender = message -> { throw new TemporaryEmailException("throttling"); };
        EmailOutboxTransactionService tx = mock(EmailOutboxTransactionService.class); when(tx.claim()).thenReturn(List.of(job));
        new EmailWorker(sender, crypto, p, new ObjectMapper(), new EmailRateLimiter(p), tx, Runnable::run).tick();
        verify(tx).retry(eq(job), anyString(), eq(1), any()); verify(tx, never()).failed(any(), anyString());
    }

    private EmailProperties properties(){EmailProperties p=new EmailProperties();p.setMaxPerSecond(100);p.setBatchSize(10);p.setMaxRetries(5);p.setTokenEncryptionKey("test");return p;}
    private EmailOutboxJpaEntity job(EmailPayloadCrypto crypto, EmailProperties p){EmailOutboxJpaEntity j=new EmailOutboxJpaEntity();j.setType(es.aelb.backendweb.application.email.EmailType.PASSWORD_RESET);j.setRecipient("test@example.com");j.setPayload(crypto.encrypt("{\"name\":\"Test\",\"token\":\"abc\"}"));j.setNextAttemptAt(LocalDateTime.now());return j;}
}
