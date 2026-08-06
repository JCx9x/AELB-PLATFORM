package es.aelb.backendweb.application.quota;
import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.shared.exception.DomainException;
public class MarkQuotaPaidUseCase {
    private final QuotaRepository repository;
    public MarkQuotaPaidUseCase(QuotaRepository repository) { this.repository = repository; }
    public Quota execute(String userId, int year, String markedByUserId) {
        Quota quota = repository.findByUserAndYear(es.aelb.backendweb.domain.user.valueobject.UserId.of(userId), year).orElseThrow(() -> new NotFoundException());
        quota.markAsPaidManually(es.aelb.backendweb.domain.user.valueobject.UserId.of(markedByUserId)); repository.save(quota); return quota;
    }
    public static final class NotFoundException extends DomainException { public NotFoundException() { super("Cuota no encontrada"); } }
}
