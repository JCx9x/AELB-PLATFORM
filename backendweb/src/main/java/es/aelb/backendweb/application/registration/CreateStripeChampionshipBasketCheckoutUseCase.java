package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.category.*;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.*;
import es.aelb.backendweb.domain.pricing.checkout.CheckoutLineItem;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.registration.ChampionshipCategorySelectionPolicy;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.quota.StripeCheckoutGateway;
import es.aelb.backendweb.domain.quota.StripeCheckoutSession;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/** Crea un pago único para todas las categorías elegidas por el atleta. */
public class CreateStripeChampionshipBasketCheckoutUseCase {
    private final UserRepository users; private final ChampionshipRepository championships; private final CategoryRepository categories;
    private final QuotaRepository quotas; private final ChampionshipAgeCategoryPriceRepository prices;
    private final ChampionshipRegistrationPriceCalculator calculator; private final RegistrationCheckoutRepository checkouts; private final StripeCheckoutGateway stripe; private final RegistrationRepository registrations;
    public CreateStripeChampionshipBasketCheckoutUseCase(UserRepository users, ChampionshipRepository championships, CategoryRepository categories, QuotaRepository quotas, ChampionshipAgeCategoryPriceRepository prices, ChampionshipRegistrationPriceCalculator calculator, RegistrationCheckoutRepository checkouts, StripeCheckoutGateway stripe, RegistrationRepository registrations) { this.users = users; this.championships = championships; this.categories = categories; this.quotas = quotas; this.prices = prices; this.calculator = calculator; this.checkouts = checkouts; this.stripe = stripe; this.registrations = registrations; }

    public Quote quote(String userId, String championshipId, List<String> categoryIds) {
        User user = users.findById(UserId.of(userId)).orElseThrow(() -> new RegisterForChampionshipUseCase.UserNotFoundException(userId));
        if (user.isBlocked()) throw new User.UserBlockedException(userId);
        Championship championship = championships.findById(ChampionshipId.of(championshipId)).orElseThrow(() -> new RegisterForChampionshipUseCase.ChampionshipNotFoundException(championshipId));
        if (!championship.isRegistrationOpen()) throw new Championship.RegistrationClosedException(championshipId);
        if (championship.requiresCurrentQuota() && !quotas.findByUserAndYear(UserId.of(userId), LocalDate.now().getYear()).map(q -> q.isPaid()).orElse(false)) throw new RegisterForChampionshipUseCase.CurrentQuotaRequiredException(LocalDate.now().getYear());
        if (new HashSet<>(categoryIds).size() != categoryIds.size()) throw new InvalidBasketException("No puedes seleccionar la misma categoría más de una vez.");
        List<Category> selected = categoryIds.stream().map(id -> {
            if (!championship.hasCategory(id)) throw new RegisterForChampionshipUseCase.CategoryNotInChampionshipException(id, championshipId);
            Category category = categories.findById(CategoryId.of(id)).orElseThrow(() -> new RegisterForChampionshipUseCase.CategoryNotInChampionshipException(id, championshipId));
            if (category.getAgeCategory() != null && !category.getAgeCategory().acceptsBirthDateForCompetitionYear(user.getBirthDate(), championship.getEventDate().getYear())) throw new RegisterForChampionshipUseCase.AgeCategoryNotEligibleException(category.getAgeCategory().getDisplayName(), championship.getEventDate().getYear());
            return category;
        }).toList();
        List<Category> existing = registrations.findByUserAndChampionship(UserId.of(userId), ChampionshipId.of(championshipId)).stream()
                .map(Registration::getCategoryId)
                .map(CategoryId::of)
                .map(categories::findById)
                .flatMap(Optional::stream)
                .toList();
        if (existing.stream().anyMatch(category -> categoryIds.contains(category.getId().value()))) {
            throw new RegisterForChampionshipUseCase.DuplicateRegistrationException(userId, championshipId);
        }
        List<Category> allSelections = new ArrayList<>(existing);
        allSelections.addAll(selected);
        if (allSelections.size() > 4) throw new RegisterForChampionshipUseCase.MaxRegistrationsExceededException(userId);
        if (!ChampionshipCategorySelectionPolicy.isValid(allSelections)) {
            throw new RegisterForChampionshipUseCase.SameShiftConflictException(null);
        }
        return new Quote(calculator.calculate(selected, prices.findByChampionshipId(ChampionshipId.of(championshipId))), selected);
    }
    public StripeCheckoutSession create(String userId, String championshipId, List<String> categoryIds) {
        Quote quote = quote(userId, championshipId, categoryIds);
        RegistrationCheckout checkout = reserveCheckout(userId, championshipId, categoryIds, quote);
        StripeCheckoutSession session = stripe.createFor(checkout, "Inscripción campeonato");
        checkout.bindStripeSession(session.id());
        checkouts.save(checkout);
        return session;
    }

    /**
     * A single database-backed basket key reserves this exact selection while it is pending.
     * The database unique index is the final protection when two HTTP requests race each other.
     */
    private RegistrationCheckout reserveCheckout(String userId, String championshipId, List<String> categoryIds, Quote quote) {
        String basketKey = basketKey(userId, championshipId, categoryIds);
        Optional<RegistrationCheckout> existing = checkouts.findByActiveBasketKey(basketKey);
        if (existing.isPresent()) {
            RegistrationCheckout checkout = existing.get();
            if (!checkout.isExpired()) return checkout;
            checkout.expire();
            checkouts.save(checkout);
        }

        String names = quote.categories.stream().map(Category::getName).collect(java.util.stream.Collectors.joining(", "));
        RegistrationCheckout created = RegistrationCheckout.create(userId, championshipId,
                List.of(CheckoutLineItem.of("Inscripción: " + names, quote.total, "championship-basket")), quote.total, "EUR",
                Map.of("categoryIds", String.join(",", categoryIds)), basketKey);
        try {
            checkouts.save(created);
            return created;
        } catch (RegistrationCheckout.DuplicateActiveBasketException ignored) {
            return checkouts.findByActiveBasketKey(basketKey)
                    .orElseThrow(RegistrationCheckout.DuplicateActiveBasketException::new);
        }
    }

    private String basketKey(String userId, String championshipId, List<String> categoryIds) {
        try {
            List<String> sortedIds = categoryIds.stream().sorted().toList();
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((userId + ':' + championshipId + ':' + String.join(",", sortedIds)).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
    public record Quote(BigDecimal total, List<Category> categories) {}
    public static final class InvalidBasketException extends DomainException { public InvalidBasketException(String message) { super(message); } }
}
