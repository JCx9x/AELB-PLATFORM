package es.aelb.backendweb.application.quota;

import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.StripeCheckoutGateway;
import es.aelb.backendweb.domain.quota.StripeCheckoutSession;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import org.springframework.transaction.annotation.Transactional;

public class CreateStripeQuotaCheckoutUseCase {
    private final GetOrCreateUserQuotaUseCase getOrCreateQuota;
    private final es.aelb.backendweb.domain.quota.QuotaRepository quotaRepository;
    private final StripeCheckoutGateway stripeCheckoutGateway;

    public CreateStripeQuotaCheckoutUseCase(GetOrCreateUserQuotaUseCase getOrCreateQuota, es.aelb.backendweb.domain.quota.QuotaRepository quotaRepository, StripeCheckoutGateway stripeCheckoutGateway) {
        this.getOrCreateQuota = getOrCreateQuota;
        this.quotaRepository = quotaRepository;
        this.stripeCheckoutGateway = stripeCheckoutGateway;
    }

    @Transactional
    public StripeCheckoutSession execute(String userId, int year) {
        // El bloqueo cubre la comprobación, la creación idempotente en Stripe y la persistencia.
        Quota quota = getOrCreateQuota.execute(userId, year);
        quota = quotaRepository.findByUserAndYearForUpdate(UserId.of(userId), year)
                .orElse(quota);
        if (quota.isPaid()) throw new IllegalStateException("La cuota de " + year + " ya está pagada");
        if (quota.hasPendingStripeSession()) {
            return new StripeCheckoutSession(quota.getStripeSessionId(), quota.getStripeSessionUrl());
        }
        StripeCheckoutSession session = stripeCheckoutGateway.createFor(quota);
        quota.bindStripeSession(session.id(), session.url());
        quotaRepository.save(quota);
        return session;
    }
}
