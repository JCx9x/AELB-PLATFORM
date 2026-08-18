package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import org.springframework.transaction.annotation.Transactional;
import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;

/** Confirma las reservas ya creadas cuando llega el webhook firmado de Stripe. */
public class ConfirmStripeChampionshipBasketPaymentUseCase {
    private final RegistrationCheckoutRepository checkouts; private final RegistrationRepository registrationRepository; private final UserRepository users; private final ChampionshipRepository championships; private final EmailService emails;
    public ConfirmStripeChampionshipBasketPaymentUseCase(RegistrationCheckoutRepository checkouts, RegistrationRepository registrationRepository) { this(checkouts,registrationRepository,null,null,EmailService.NOOP); }
    public ConfirmStripeChampionshipBasketPaymentUseCase(RegistrationCheckoutRepository checkouts, RegistrationRepository registrationRepository, UserRepository users, ChampionshipRepository championships, EmailService emails) { this.checkouts = checkouts; this.registrationRepository = registrationRepository; this.users=users;this.championships=championships;this.emails=emails; }
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
        if (users != null) users.findById(UserId.of(checkout.getUserId())).ifPresent(user -> championships.findById(ChampionshipId.of(checkout.getChampionshipId())).ifPresent(champ -> emails.requestPaymentConfirmation(user.getEmail().value(), user.getFullName(), checkout.getId(), "Inscripciones en " + champ.getName(), checkout.getTotal().toPlainString() + " EUR")));
    }
}
