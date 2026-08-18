package es.aelb.backendweb.application.quota;

import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import org.springframework.transaction.annotation.Transactional;

/** Confirma de forma idempotente el pago recibido en un webhook verificado de Stripe. */
public class ConfirmStripeQuotaPaymentUseCase {
    private final QuotaRepository repository;
    public ConfirmStripeQuotaPaymentUseCase(QuotaRepository repository) { this.repository = repository; }

    @Transactional
    public void execute(String userId, int year, String checkoutSessionId, long amountTotalInCents) {
        if (checkoutSessionId == null || checkoutSessionId.isBlank() || amountTotalInCents < 0) {
            throw new InvalidStripePaymentException();
        }
        Quota quota = repository.findByStripeSessionIdForUpdate(checkoutSessionId)
                .orElseThrow(() -> new MarkQuotaPaidUseCase.NotFoundException());
        if (!quota.getUserId().equals(UserId.of(userId)) || quota.getYear() != year) {
            throw new InvalidStripePaymentException();
        }
        long expectedAmountInCents;
        try {
            expectedAmountInCents = quota.getAmount().movePointRight(2).longValueExact();
        } catch (ArithmeticException ex) {
            throw new InvalidStripePaymentException();
        }
        if (expectedAmountInCents != amountTotalInCents) {
            throw new InvalidStripePaymentException();
        }
        if (!quota.isPaid()) {
            quota.markAsPaidByStripe(checkoutSessionId);
            repository.save(quota);
        }
    }

    public static final class InvalidStripePaymentException extends RuntimeException {
        public InvalidStripePaymentException() { super("El pago de Stripe no coincide con la cuota pendiente"); }
    }
}
