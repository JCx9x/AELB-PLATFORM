package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;
import es.aelb.backendweb.domain.quota.*;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.QuotaJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringQuotaJpaRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component
public class JpaQuotaRepositoryAdapter implements QuotaRepository {
    private final SpringQuotaJpaRepository repository;
    public JpaQuotaRepositoryAdapter(SpringQuotaJpaRepository repository) { this.repository = repository; }
    public void save(Quota q) { repository.save(toJpa(q)); }
    public Optional<Quota> findByUserAndYear(UserId id, int year) { return repository.findByUserIdAndYear(id.value(), year).map(this::toDomain); }
    public boolean existsByUserAndYear(UserId id, int year) { return repository.existsByUserIdAndYear(id.value(), year); }
    public List<Quota> findByUser(UserId id) { return repository.findByUserIdOrderByYearDesc(id.value()).stream().map(this::toDomain).toList(); }
    public List<String> findPaidUserIdsByYear(int year) { return repository.findPaidUserIdsByYear(year); }
    private QuotaJpaEntity toJpa(Quota q) { QuotaJpaEntity e = new QuotaJpaEntity(); e.setId(q.getId()); e.setUserId(q.getUserId().value()); e.setYear(q.getYear()); e.setAgeCategory(QuotaJpaEntity.AgeCategoryJpa.valueOf(q.getAgeCategory().name())); e.setAmount(q.getAmount()); e.setPaymentStatus(QuotaJpaEntity.PaymentStatusJpa.valueOf(q.getStatus().name())); e.setPaymentDate(q.getPaymentDate()); e.setPaidAt(q.getPaidAt()); e.setPaymentSource(q.getPaymentSource() == null ? null : QuotaJpaEntity.PaymentSourceJpa.valueOf(q.getPaymentSource().name())); e.setPaidByUserId(q.getPaidByUserId() == null ? null : q.getPaidByUserId().value()); e.setPaymentReference(q.getPaymentReference()); e.setCreatedAt(q.getCreatedAt()); e.setUpdatedAt(q.getUpdatedAt()); return e; }
    private Quota toDomain(QuotaJpaEntity e) { return Quota.reconstitute(e.getId(), UserId.of(e.getUserId()), e.getYear(), AgeCategory.valueOf(e.getAgeCategory().name()), e.getAmount(), Quota.QuotaPaymentStatus.valueOf(e.getPaymentStatus().name()), e.getPaymentDate(), e.getPaidAt(), e.getPaymentSource() == null ? null : Quota.PaymentSource.valueOf(e.getPaymentSource().name()), e.getPaidByUserId() == null ? null : UserId.of(e.getPaidByUserId()), e.getPaymentReference(), e.getCreatedAt(), e.getUpdatedAt()); }
}
