package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.domain.pricing.checkout.CheckoutLineItem;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.quota.StripeCheckoutGateway;
import es.aelb.backendweb.domain.quota.StripeCheckoutSession;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

public class CreateStripeRegistrationCheckoutUseCase {
    private final RegistrationRepository registrations;
    private final RegistrationCheckoutRepository checkouts;
    private final CategoryRepository categories;
    private final StripeCheckoutGateway stripe;
    public CreateStripeRegistrationCheckoutUseCase(RegistrationRepository registrations, RegistrationCheckoutRepository checkouts, CategoryRepository categories, StripeCheckoutGateway stripe) { this.registrations = registrations; this.checkouts = checkouts; this.categories = categories; this.stripe = stripe; }

    public StripeCheckoutSession execute(String registrationId, String userId) {
        Registration registration = registrations.findById(registrationId).orElseThrow(() -> new Registration.NotFoundException(registrationId));
        if (!registration.getUserId().equals(UserId.of(userId))) throw new ForbiddenRegistrationPaymentException();
        if (registration.isPaid()) throw new AlreadyPaidException();
        if (registration.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new InvalidAmountException();
        Category category = categories.findById(CategoryId.of(registration.getCategoryId())).orElseThrow(() -> new Registration.NotFoundException(registrationId));
        String basketKey = registrationBasketKey(registrationId);
        Optional<RegistrationCheckout> existing = checkouts.findByActiveBasketKey(basketKey);
        if (existing.isPresent() && !existing.get().isExpired()) {
            return createOrReuseStripeSession(existing.get(), "Inscripción - " + category.getName());
        }
        if (existing.isPresent()) {
            existing.get().expire();
            checkouts.save(existing.get());
        }
        RegistrationCheckout checkout = RegistrationCheckout.create(userId, registration.getChampionshipId().value(),
                java.util.List.of(CheckoutLineItem.of("Inscripción " + category.getName(), registration.getAmount(), registrationId)),
                registration.getAmount(), "EUR", Map.of("registrationId", registrationId), basketKey);
        try {
            checkouts.save(checkout);
            return createOrReuseStripeSession(checkout, "Inscripción - " + category.getName());
        } catch (RegistrationCheckout.DuplicateActiveBasketException ignored) {
            return createOrReuseStripeSession(checkouts.findByActiveBasketKey(basketKey)
                    .orElseThrow(RegistrationCheckout.DuplicateActiveBasketException::new), "Inscripción - " + category.getName());
        }
    }

    private StripeCheckoutSession createOrReuseStripeSession(RegistrationCheckout checkout, String description) {
        StripeCheckoutSession session = stripe.createFor(checkout, description);
        checkout.bindStripeSession(session.id());
        checkouts.save(checkout);
        return session;
    }

    private String registrationBasketKey(String registrationId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("registration:" + registrationId).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
    public static final class ForbiddenRegistrationPaymentException extends DomainException { public ForbiddenRegistrationPaymentException() { super("No puedes pagar esta inscripción."); } }
    public static final class AlreadyPaidException extends DomainException { public AlreadyPaidException() { super("Esta inscripción ya está pagada."); } }
    public static final class InvalidAmountException extends DomainException { public InvalidAmountException() { super("La inscripción no tiene un importe pendiente válido."); } }
}
