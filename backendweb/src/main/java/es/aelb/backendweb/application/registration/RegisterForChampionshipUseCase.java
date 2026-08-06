package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.Shift;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.Championship.RegistrationClosedException;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.registration.ChampionshipCategorySelectionPolicy;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.pricing.ChampionshipAgeCategoryPriceRepository;
import es.aelb.backendweb.domain.pricing.ChampionshipRegistrationPriceCalculator;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.User.UserBlockedException;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.domain.shared.exception.DomainException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Inscribe un usuario en una categoría de un campeonato.
 *
 * Reglas por turno (MORNING / AFTERNOON):
 * - Se permiten hasta 2 inscripciones por turno.
 * - La segunda inscripción en el mismo turno solo es válida si:
 *     · La categoría nueva y la existente tienen el mismo género, límite de peso y grupo de edad.
 *     · Los lados del brazo son diferentes (LEFT ↔ RIGHT).
 *     · Ninguna de las dos usa el lado BOTH.
 * Ejemplo válido: master -75 kg LEFT (mañana) + master -75 kg RIGHT (mañana).
 */
public class RegisterForChampionshipUseCase implements UseCase<RegisterForChampionshipCommand, String> {

    private final UserRepository         userRepository;
    private final ChampionshipRepository championshipRepository;
    private final RegistrationRepository registrationRepository;
    private final CategoryRepository     categoryRepository;
    private final QuotaRepository        quotaRepository;
    private final ChampionshipAgeCategoryPriceRepository categoryPriceRepository;
    private final ChampionshipRegistrationPriceCalculator priceCalculator;

    public RegisterForChampionshipUseCase(
            UserRepository userRepository,
            ChampionshipRepository championshipRepository,
            RegistrationRepository registrationRepository,
            CategoryRepository categoryRepository,
            QuotaRepository quotaRepository,
            ChampionshipAgeCategoryPriceRepository categoryPriceRepository,
            ChampionshipRegistrationPriceCalculator priceCalculator
    ) {
        this.userRepository         = userRepository;
        this.championshipRepository = championshipRepository;
        this.registrationRepository = registrationRepository;
        this.categoryRepository     = categoryRepository;
        this.quotaRepository        = quotaRepository;
        this.categoryPriceRepository = categoryPriceRepository;
        this.priceCalculator = priceCalculator;
    }

    @Override
    public String execute(RegisterForChampionshipCommand cmd) {
        UserId         userId         = UserId.of(cmd.userId());
        ChampionshipId championshipId = ChampionshipId.of(cmd.championshipId());
        UserId         registeredById = UserId.of(cmd.registeredById());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(cmd.userId()));

        if (user.isBlocked()) {
            throw new UserBlockedException(cmd.userId());
        }

        Championship championship = championshipRepository.findById(championshipId)
                .orElseThrow(() -> new ChampionshipNotFoundException(cmd.championshipId()));

        if (!championship.isRegistrationOpen()) {
            throw new RegistrationClosedException(cmd.championshipId());
        }

        if (championship.requiresCurrentQuota()) {
            boolean quotaPaid = quotaRepository.findByUserAndYear(userId, LocalDate.now().getYear())
                    .map(q -> q.isPaid())
                    .orElse(false);
            if (!quotaPaid) throw new CurrentQuotaRequiredException(LocalDate.now().getYear());
        }

        if (!championship.hasCategory(cmd.categoryId())) {
            throw new CategoryNotInChampionshipException(cmd.categoryId(), cmd.championshipId());
        }

        Category requestedCategory = categoryRepository.findById(CategoryId.of(cmd.categoryId()))
                .orElseThrow(() -> new CategoryNotInChampionshipException(cmd.categoryId(), cmd.championshipId()));

        if (requestedCategory.getAgeCategory() != null
                && !requestedCategory.getAgeCategory().acceptsBirthDateForCompetitionYear(user.getBirthDate(), championship.getEventDate().getYear())) {
            throw new AgeCategoryNotEligibleException(requestedCategory.getAgeCategory().getDisplayName(), championship.getEventDate().getYear());
        }

        Shift requestedShift = requestedCategory.getShift();

        List<Registration> existing = registrationRepository.findByUserAndChampionship(userId, championshipId);

        if (existing.size() >= 4) {
            throw new MaxRegistrationsExceededException(cmd.userId());
        }

        // Reject if user is already registered in the exact same category
        for (Registration reg : existing) {
            if (reg.getCategoryId().equals(cmd.categoryId())) {
                throw new DuplicateRegistrationException(cmd.userId(), cmd.championshipId());
            }
        }

        // Collect categories of existing registrations in the same shift
        List<Category> sameShiftCategories = new ArrayList<>();
        for (Registration reg : existing) {
            Optional<Category> catOpt = categoryRepository.findById(CategoryId.of(reg.getCategoryId()));
            if (catOpt.isPresent() && catOpt.get().getShift() == requestedShift) {
                sameShiftCategories.add(catOpt.get());
            }
        }

        if (!ChampionshipCategorySelectionPolicy.canAdd(sameShiftCategories, requestedCategory)) {
            throw new SameShiftConflictException(requestedShift);
        }

        List<Category> previousCategories = existing.stream()
                .map(reg -> categoryRepository.findById(CategoryId.of(reg.getCategoryId())))
                .flatMap(Optional::stream)
                .toList();
        List<Category> quotedCategories = new ArrayList<>(previousCategories);
        quotedCategories.add(requestedCategory);
        BigDecimal previousTotal = previousCategories.isEmpty() ? BigDecimal.ZERO : priceCalculator.calculate(previousCategories, categoryPriceRepository.findByChampionshipId(championshipId));
        BigDecimal newTotal = priceCalculator.calculate(quotedCategories, categoryPriceRepository.findByChampionshipId(championshipId));

        Registration registration = Registration.create(userId, championshipId, cmd.categoryId(), newTotal.subtract(previousTotal), registeredById);
        registrationRepository.save(registration);
        return registration.getId();
    }

    public static final class UserNotFoundException extends DomainException {
        public UserNotFoundException(String id) {
            super("Usuario no encontrado: " + id);
        }
    }

    public static final class ChampionshipNotFoundException extends DomainException {
        public ChampionshipNotFoundException(String id) {
            super("Campeonato no encontrado: " + id);
        }
    }

    public static final class CategoryNotInChampionshipException extends DomainException {
        public CategoryNotInChampionshipException(String categoryId, String championshipId) {
            super("La categoría " + categoryId + " no pertenece al campeonato " + championshipId);
        }
    }

    public static final class DuplicateRegistrationException extends DomainException {
        public DuplicateRegistrationException(String userId, String championshipId) {
            super("El usuario " + userId + " ya está inscrito en esa categoría del campeonato " + championshipId);
        }
    }

    public static final class SameShiftConflictException extends DomainException {
        public SameShiftConflictException(Shift shift) {
            super(shift == null
                    ? "En cada turno solo puedes seleccionar la misma categoría base en brazos diferentes (izquierdo y derecho)."
                    : "Conflicto de turno en " + (shift == Shift.MORNING ? "mañana" : "tarde")
                    + ". Solo puedes inscribirte en dos categorías del mismo turno si son la misma categoría base en brazos diferentes (izquierdo y derecho).");
        }
    }

    public static final class MaxRegistrationsExceededException extends DomainException {
        public MaxRegistrationsExceededException(String userId) {
            super("El usuario " + userId + " ya tiene el máximo de 4 inscripciones en este campeonato.");
        }
    }

    public static final class CurrentQuotaRequiredException extends DomainException {
        public CurrentQuotaRequiredException(int year) {
            super("Este campeonato requiere tener pagada la cuota anual vigente (" + year + ") antes de inscribirse.");
        }
    }

    public static final class AgeCategoryNotEligibleException extends DomainException {
        public AgeCategoryNotEligibleException(String category, int competitionYear) {
            super("No cumples la edad requerida para la categoría " + category + " en el año de competición " + competitionYear + ".");
        }
    }
}
