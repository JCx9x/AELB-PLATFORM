package es.aelb.backendweb.infrastructure.email;
import es.aelb.backendweb.application.email.EmailSender; import es.aelb.backendweb.infrastructure.config.EmailProperties; import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration
public class EmailSenderConfiguration {
 @Bean(name="emailWorkerExecutor") ThreadPoolTaskExecutor emailWorkerExecutor(EmailProperties p){ThreadPoolTaskExecutor e=new ThreadPoolTaskExecutor();e.setCorePoolSize(p.getWorkerConcurrency());e.setMaxPoolSize(p.getWorkerConcurrency());e.setQueueCapacity(Math.max(p.getBatchSize(),10));e.setThreadNamePrefix("email-worker-");e.initialize();return e;}
 @Bean @Profile({"local","dev"}) EmailSender localEmailSender(@Qualifier("smtpEmailSender") EmailSender smtp){return smtp;}
 @Bean @Profile("prod") EmailSender productionEmailSender(@Qualifier("sesEmailSender") EmailSender ses){return ses;}
 @Bean @Profile("test") EmailSender testEmailSender(@Qualifier("fakeEmailSender") EmailSender fake){return fake;}
}
