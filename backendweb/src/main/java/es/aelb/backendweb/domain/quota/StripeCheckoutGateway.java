package es.aelb.backendweb.domain.quota;

import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;

/** Puerto para crear una página de pago alojada por Stripe. */
public interface StripeCheckoutGateway {
    StripeCheckoutSession createFor(Quota quota);
    StripeCheckoutSession createFor(RegistrationCheckout checkout, String description);
}
