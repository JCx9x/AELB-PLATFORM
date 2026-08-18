package es.aelb.backendweb.infrastructure.persistence.jpa.repository;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.PasswordResetTokenJpaEntity; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.LocalDateTime; import java.util.*;
public interface SpringPasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity,String> {
 Optional<PasswordResetTokenJpaEntity> findByTokenHash(String hash); void deleteByUserId(String userId);
 boolean existsByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);
 long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);
}
