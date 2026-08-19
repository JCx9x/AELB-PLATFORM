package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import org.springframework.transaction.annotation.Transactional;
import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Confirma el checkout y la inscripción a partir de un webhook Stripe ya verificado. */
public class ConfirmStripeRegistrationPaymentUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConfirmStripeRegistrationPaymentUseCase.class);
    private final RegistrationCheckoutRepository checkouts; private final RegistrationRepository registrations; private final UserRepository users; private final ChampionshipRepository championships; private final EmailService emails;
    public ConfirmStripeRegistrationPaymentUseCase(RegistrationCheckoutRepository checkouts, RegistrationRepository registrations, UserRepository users, ChampionshipRepository championships, EmailService emails) { this.checkouts = checkouts; this.registrations = registrations; this.users=users;this.championships=championships;this.emails=emails; }
    @Transactional
    public void execute(String checkoutId, String stripeSessionId) {
        RegistrationCheckout checkout = checkouts.findByIdForUpdate(checkoutId).orElseThrow(() -> new RegistrationCheckout.NotFoundException(checkoutId));
        checkout.validateStripeSession(stripeSessionId);
        if (checkout.isPaid()) { log.info("event=PAYMENT_ALREADY_CONFIRMED checkoutId={} stripeSessionId={}", checkoutId, stripeSessionId); return; }
        String registrationId = checkout.getMetadata().get("registrationId");
        Registration registration = registrations.findById(registrationId).orElseThrow(() -> new Registration.NotFoundException(registrationId));
        if (!registration.isPaid()) { registration.confirmPayment(); registrations.save(registration); }
        checkout.confirmPayment(stripeSessionId);
        checkouts.save(checkout);
        log.info("event=PAYMENT_CONFIRMED paymentId={} registrationId={} stripeSessionId={}", checkoutId, registrationId, stripeSessionId);
        users.findById(registration.getUserId()).ifPresent(user -> championships.findById(registration.getChampionshipId()).ifPresent(championship -> emails.requestPaymentConfirmation(user.getEmail().value(), user.getFullName(), registration.getId(), "Inscripción en " + championship.getName(), registration.getAmount().toPlainString() + " EUR")));
    }
}
