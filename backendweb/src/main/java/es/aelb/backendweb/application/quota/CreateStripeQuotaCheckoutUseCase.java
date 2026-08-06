package es.aelb.backendweb.application.quota;

import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.StripeCheckoutGateway;
import es.aelb.backendweb.domain.quota.StripeCheckoutSession;

public class CreateStripeQuotaCheckoutUseCase {
    private final GetOrCreateUserQuotaUseCase getOrCreateQuota;
    private final StripeCheckoutGateway stripeCheckoutGateway;

    public CreateStripeQuotaCheckoutUseCase(GetOrCreateUserQuotaUseCase getOrCreateQuota, StripeCheckoutGateway stripeCheckoutGateway) {
        this.getOrCreateQuota = getOrCreateQuota;
        this.stripeCheckoutGateway = stripeCheckoutGateway;
    }

    public StripeCheckoutSession execute(String userId, int year) {
        Quota quota = getOrCreateQuota.execute(userId, year);
        if (quota.isPaid()) throw new IllegalStateException("La cuota de " + year + " ya está pagada");
        return stripeCheckoutGateway.createFor(quota);
    }
}
