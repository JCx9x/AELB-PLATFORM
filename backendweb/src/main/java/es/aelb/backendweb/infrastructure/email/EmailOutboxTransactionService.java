package es.aelb.backendweb.infrastructure.email;
import es.aelb.backendweb.infrastructure.config.EmailProperties; import es.aelb.backendweb.infrastructure.persistence.jpa.entity.EmailOutboxJpaEntity; import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringEmailOutboxJpaRepository; import org.springframework.data.domain.PageRequest; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.LocalDateTime; import java.util.List;
@Service public class EmailOutboxTransactionService {
 private final SpringEmailOutboxJpaRepository repo; private final EmailProperties props; public EmailOutboxTransactionService(SpringEmailOutboxJpaRepository repo,EmailProperties props){this.repo=repo;this.props=props;}
 @Transactional public List<EmailOutboxJpaEntity> claim(){int limit=Math.max(1,Math.min(props.getBatchSize(),props.getWorkerConcurrency()));List<EmailOutboxJpaEntity> jobs=repo.findPendingForUpdate(LocalDateTime.now(),PageRequest.of(0,limit));LocalDateTime now=LocalDateTime.now();jobs.forEach(j->{j.setStatus(EmailOutboxJpaEntity.Status.PROCESSING);j.setProcessingStartedAt(now);});return repo.saveAll(jobs);}
 @Transactional public void recover(){repo.recoverAbandoned(LocalDateTime.now().minusMinutes(props.getProcessingTimeoutMinutes()));}
 @Transactional public void cleanSent(){repo.deleteSentBefore(LocalDateTime.now().minusDays(props.getSentRetentionDays()));}
 @Transactional public void sent(EmailOutboxJpaEntity j){j.setStatus(EmailOutboxJpaEntity.Status.SENT);j.setSentAt(LocalDateTime.now());j.setProcessingStartedAt(null);repo.save(j);}
 @Transactional public void pending(EmailOutboxJpaEntity j,String error,LocalDateTime next){j.setStatus(EmailOutboxJpaEntity.Status.PENDING);j.setNextAttemptAt(next);j.setProcessingStartedAt(null);j.setLastError(error);repo.save(j);}
 @Transactional public void failed(EmailOutboxJpaEntity j,String error){j.setStatus(EmailOutboxJpaEntity.Status.FAILED);j.setProcessingStartedAt(null);j.setLastError(error);repo.save(j);}
 @Transactional public void retry(EmailOutboxJpaEntity j,String error,int attempts,LocalDateTime next){j.setAttempts(attempts);j.setStatus(EmailOutboxJpaEntity.Status.PENDING);j.setNextAttemptAt(next);j.setProcessingStartedAt(null);j.setLastError(error);repo.save(j);}
}
