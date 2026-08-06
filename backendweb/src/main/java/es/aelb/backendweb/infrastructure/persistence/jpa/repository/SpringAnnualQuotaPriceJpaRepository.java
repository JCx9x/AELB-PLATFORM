package es.aelb.backendweb.infrastructure.persistence.jpa.repository;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.AnnualQuotaPriceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SpringAnnualQuotaPriceJpaRepository extends JpaRepository<AnnualQuotaPriceJpaEntity, Long> {
    Optional<AnnualQuotaPriceJpaEntity> findByYearAndAgeCategory(int year, AnnualQuotaPriceJpaEntity.QuotaAgeCategoryJpa ageCategory);
    List<AnnualQuotaPriceJpaEntity> findByYearOrderByAgeCategory(int year);
    void deleteByYear(int year);
}
