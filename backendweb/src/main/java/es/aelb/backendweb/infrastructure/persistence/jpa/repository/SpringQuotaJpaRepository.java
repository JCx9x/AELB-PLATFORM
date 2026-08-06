package es.aelb.backendweb.infrastructure.persistence.jpa.repository;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.QuotaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface SpringQuotaJpaRepository extends JpaRepository<QuotaJpaEntity, String> {
    Optional<QuotaJpaEntity> findByUserIdAndYear(String userId, int year);
    boolean existsByUserIdAndYear(String userId, int year);
    List<QuotaJpaEntity> findByUserIdOrderByYearDesc(String userId);
    @Query("select q.userId from QuotaJpaEntity q where q.year = :year and q.paymentStatus = 'PAID'")
    List<String> findPaidUserIdsByYear(int year);
}
