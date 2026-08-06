package es.aelb.backendweb.application.quota;

import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

/** Confirma de forma idempotente el pago recibido en un webhook verificado de Stripe. */
public class ConfirmStripeQuotaPaymentUseCase {
    private final QuotaRepository repository;
    public ConfirmStripeQuotaPaymentUseCase(QuotaRepository repository) { this.repository = repository; }

    public void execute(String userId, int year, String checkoutSessionId) {
        Quota quota = repository.findByUserAndYear(UserId.of(userId), year)
                .orElseThrow(() -> new MarkQuotaPaidUseCase.NotFoundException());
        if (!quota.isPaid()) {
            quota.markAsPaidByStripe(checkoutSessionId);
            repository.save(quota);
        }
    }
}
