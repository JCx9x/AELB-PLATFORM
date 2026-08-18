package es.aelb.backendweb.infrastructure.persistence.jpa.repository;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.EmailVerificationTokenJpaEntity; import org.springframework.data.jpa.repository.*; import java.time.LocalDateTime; import java.util.*;
public interface SpringEmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenJpaEntity,String> {
 Optional<EmailVerificationTokenJpaEntity> findByTokenHash(String hash); void deleteByUserId(String userId);
 boolean existsByUserIdAndCreatedAtAfter(String userId, LocalDateTime since); long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);
}
