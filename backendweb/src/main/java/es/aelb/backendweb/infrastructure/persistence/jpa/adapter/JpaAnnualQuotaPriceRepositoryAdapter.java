package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;
import es.aelb.backendweb.domain.quota.*;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.AnnualQuotaPriceJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringAnnualQuotaPriceJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Component
public class JpaAnnualQuotaPriceRepositoryAdapter implements AnnualQuotaPriceRepository {
    private final SpringAnnualQuotaPriceJpaRepository repository;
    public JpaAnnualQuotaPriceRepositoryAdapter(SpringAnnualQuotaPriceJpaRepository repository) { this.repository = repository; }
    @Transactional public void saveAll(List<AnnualQuotaPrice> prices) { if (prices.isEmpty()) return; int year = prices.getFirst().getYear(); repository.deleteByYear(year); repository.saveAll(prices.stream().map(this::toJpa).toList()); }
    public Optional<AnnualQuotaPrice> findByYearAndAgeCategory(int year, AgeCategory category) { return repository.findByYearAndAgeCategory(year, AnnualQuotaPriceJpaEntity.QuotaAgeCategoryJpa.valueOf(category.name())).map(this::toDomain); }
    public List<AnnualQuotaPrice> findByYear(int year) { return repository.findByYearOrderByAgeCategory(year).stream().map(this::toDomain).toList(); }
    private AnnualQuotaPriceJpaEntity toJpa(AnnualQuotaPrice p) { AnnualQuotaPriceJpaEntity e = new AnnualQuotaPriceJpaEntity(); e.setYear(p.getYear()); e.setAgeCategory(AnnualQuotaPriceJpaEntity.QuotaAgeCategoryJpa.valueOf(p.getAgeCategory().name())); e.setAmount(p.getAmount()); return e; }
    private AnnualQuotaPrice toDomain(AnnualQuotaPriceJpaEntity e) { return new AnnualQuotaPrice(e.getYear(), AgeCategory.valueOf(e.getAgeCategory().name()), e.getAmount()); }
}
