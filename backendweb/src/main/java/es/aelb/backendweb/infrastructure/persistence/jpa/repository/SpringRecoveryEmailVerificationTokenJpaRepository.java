package es.aelb.backendweb.infrastructure.persistence.jpa.repository;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RecoveryEmailVerificationTokenJpaEntity; import org.springframework.data.jpa.repository.*; import java.time.LocalDateTime; import java.util.*;
public interface SpringRecoveryEmailVerificationTokenJpaRepository extends JpaRepository<RecoveryEmailVerificationTokenJpaEntity,String> {
 Optional<RecoveryEmailVerificationTokenJpaEntity> findByTokenHash(String hash); void deleteByUserId(String userId);
 boolean existsByUserIdAndRecoveryEmailAndCreatedAtAfter(String userId, String recoveryEmail, LocalDateTime since);
 long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);
}
