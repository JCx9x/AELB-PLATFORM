package es.aelb.backendweb.application.quota;

import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import org.springframework.transaction.annotation.Transactional;
import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Confirma de forma idempotente el pago recibido en un webhook verificado de Stripe. */
public class ConfirmStripeQuotaPaymentUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConfirmStripeQuotaPaymentUseCase.class);
    private final QuotaRepository repository;
    private final UserRepository users; private final EmailService emails;
    public ConfirmStripeQuotaPaymentUseCase(QuotaRepository repository, UserRepository users, EmailService emails) { this.repository = repository; this.users=users;this.emails=emails; }
    public ConfirmStripeQuotaPaymentUseCase(QuotaRepository repository) { this(repository, null, EmailService.NOOP); }

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
            if (users != null) users.findById(quota.getUserId()).ifPresent(user -> emails.requestPaymentConfirmation(user.getEmail().value(), user.getFullName(), quota.getId(), "Cuota anual " + quota.getYear(), quota.getAmount().toPlainString() + " EUR"));
            log.info("event=PAYMENT_CONFIRMED paymentId={} stripeSessionId={} paymentType=quota", quota.getId(), checkoutSessionId);
        } else log.info("event=PAYMENT_ALREADY_CONFIRMED paymentId={} stripeSessionId={} paymentType=quota", quota.getId(), checkoutSessionId);
    }

    public static final class InvalidStripePaymentException extends RuntimeException {
        public InvalidStripePaymentException() { super("El pago de Stripe no coincide con la cuota pendiente"); }
    }
}
