package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.EmailOutboxJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringEmailOutboxJpaRepository extends JpaRepository<EmailOutboxJpaEntity, String> {
    boolean existsByIdempotencyKey(String key);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailOutboxJpaEntity e where e.status = 'PENDING' and e.nextAttemptAt <= :now order by e.createdAt asc")
    List<EmailOutboxJpaEntity> findPendingForUpdate(@Param("now") LocalDateTime now, org.springframework.data.domain.Pageable pageable);
    @Modifying
    @Query("update EmailOutboxJpaEntity e set e.status = 'PENDING', e.processingStartedAt = null where e.status = 'PROCESSING' and e.processingStartedAt < :cutoff")
    int recoverAbandoned(@Param("cutoff") LocalDateTime cutoff);
    long countByStatus(EmailOutboxJpaEntity.Status status);
    Optional<EmailOutboxJpaEntity> findByIdempotencyKey(String key);
    @Modifying
    @Query(value = "INSERT IGNORE INTO email_outbox (id,type,recipient,payload,status,attempts,next_attempt_at,created_at,idempotency_key) VALUES (:id,:type,:recipient,:payload,'PENDING',0,:now,:now,:key)", nativeQuery = true)
    int insertIgnore(@Param("id") String id, @Param("type") String type, @Param("recipient") String recipient, @Param("payload") String payload, @Param("now") LocalDateTime now, @Param("key") String key);
    @Modifying
    @Query("delete from EmailOutboxJpaEntity e where e.status = 'SENT' and e.sentAt < :cutoff")
    int deleteSentBefore(@Param("cutoff") LocalDateTime cutoff);
}
