package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import org.springframework.transaction.annotation.Transactional;

/** Confirma las reservas ya creadas cuando llega el webhook firmado de Stripe. */
public class ConfirmStripeChampionshipBasketPaymentUseCase {
    private final RegistrationCheckoutRepository checkouts; private final RegistrationRepository registrationRepository;
    public ConfirmStripeChampionshipBasketPaymentUseCase(RegistrationCheckoutRepository checkouts, RegistrationRepository registrationRepository) { this.checkouts = checkouts; this.registrationRepository = registrationRepository; }
    @Transactional
    public void execute(String checkoutId, String stripeSessionId) {
        RegistrationCheckout checkout = checkouts.findByIdForUpdate(checkoutId).orElseThrow(() -> new RegistrationCheckout.NotFoundException(checkoutId));
        checkout.validateStripeSession(stripeSessionId);
        if (checkout.isPaid()) return;
        String reservationIds = checkout.getMetadata().get("registrationIds");
        if (reservationIds == null || reservationIds.isBlank()) throw new IllegalArgumentException("El checkout no contiene reservas de inscripción.");
        for (String reservationId : reservationIds.split(",")) {
            Registration registration = registrationRepository.findById(reservationId).orElseThrow(() -> new Registration.NotFoundException(reservationId));
            if (!registration.getUserId().equals(UserId.of(checkout.getUserId()))
                    || !registration.getChampionshipId().equals(ChampionshipId.of(checkout.getChampionshipId()))) {
                throw new IllegalArgumentException("La reserva no pertenece al checkout.");
            }
            registration.confirmPayment();
            registrationRepository.save(registration);
        }
        checkout.confirmPayment(stripeSessionId);
        checkouts.save(checkout);
    }
}
