package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import org.springframework.transaction.annotation.Transactional;

/** Confirma el checkout y la inscripción a partir de un webhook Stripe ya verificado. */
public class ConfirmStripeRegistrationPaymentUseCase {
    private final RegistrationCheckoutRepository checkouts; private final RegistrationRepository registrations;
    public ConfirmStripeRegistrationPaymentUseCase(RegistrationCheckoutRepository checkouts, RegistrationRepository registrations) { this.checkouts = checkouts; this.registrations = registrations; }
    @Transactional
    public void execute(String checkoutId, String stripeSessionId) {
        RegistrationCheckout checkout = checkouts.findByIdForUpdate(checkoutId).orElseThrow(() -> new RegistrationCheckout.NotFoundException(checkoutId));
        checkout.validateStripeSession(stripeSessionId);
        if (checkout.isPaid()) return;
        String registrationId = checkout.getMetadata().get("registrationId");
        Registration registration = registrations.findById(registrationId).orElseThrow(() -> new Registration.NotFoundException(registrationId));
        if (!registration.isPaid()) { registration.confirmPayment(); registrations.save(registration); }
        checkout.confirmPayment(stripeSessionId);
        checkouts.save(checkout);
    }
}
