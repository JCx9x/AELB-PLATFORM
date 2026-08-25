package es.aelb.backendweb.infrastructure.email;
import es.aelb.backendweb.infrastructure.config.EmailProperties; import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
// SmtpEmailSender/SesEmailSender/FakeEmailSender already self-register as the
// single EmailSender bean for their own @Profile (local/dev, prod, test
// respectively). Do not add wrapper @Bean methods here that re-expose them
// under another name: that creates two EmailSender beans for the same
// profile and breaks unqualified autowiring (e.g. EmailWorker's constructor).
@Configuration
public class EmailSenderConfiguration {
 @Bean(name="emailWorkerExecutor") ThreadPoolTaskExecutor emailWorkerExecutor(EmailProperties p){ThreadPoolTaskExecutor e=new ThreadPoolTaskExecutor();e.setCorePoolSize(p.getWorkerConcurrency());e.setMaxPoolSize(p.getWorkerConcurrency());e.setQueueCapacity(Math.max(p.getBatchSize(),10));e.setThreadNamePrefix("email-worker-");e.initialize();return e;}
}
