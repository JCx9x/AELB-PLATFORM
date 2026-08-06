package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import org.springframework.transaction.annotation.Transactional;

/** Materializa las inscripciones pagadas del basket sólo tras el webhook Stripe. */
public class ConfirmStripeChampionshipBasketPaymentUseCase {
    private final RegistrationCheckoutRepository checkouts; private final RegisterForChampionshipUseCase registrations; private final RegistrationRepository registrationRepository;
    public ConfirmStripeChampionshipBasketPaymentUseCase(RegistrationCheckoutRepository checkouts, RegisterForChampionshipUseCase registrations, RegistrationRepository registrationRepository) { this.checkouts = checkouts; this.registrations = registrations; this.registrationRepository = registrationRepository; }
    @Transactional
    public void execute(String checkoutId, String stripeSessionId) {
        RegistrationCheckout checkout = checkouts.findByIdForUpdate(checkoutId).orElseThrow(() -> new RegistrationCheckout.NotFoundException(checkoutId));
        checkout.validateStripeSession(stripeSessionId);
        if (checkout.isPaid()) return;
        String categories = checkout.getMetadata().get("categoryIds");
        if (categories == null || categories.isBlank()) throw new IllegalArgumentException("El checkout no contiene categorías de inscripción.");
        for (String categoryId : categories.split(",")) {
            String registrationId = registrations.execute(new RegisterForChampionshipCommand(checkout.getUserId(), checkout.getChampionshipId(), categoryId, checkout.getUserId()));
            Registration registration = registrationRepository.findById(registrationId).orElseThrow(() -> new Registration.NotFoundException(registrationId));
            registration.confirmPayment();
            registrationRepository.save(registration);
        }
        checkout.confirmPayment(stripeSessionId);
        checkouts.save(checkout);
    }
}
