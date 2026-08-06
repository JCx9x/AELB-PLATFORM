package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.Shift;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.registration.ChampionshipCategorySelectionPolicy;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Permite a GESTOR/ADMIN corregir la categoría de una inscripción ya
 * realizada (pagada o no), reutilizando las mismas reglas de elegibilidad
 * y de turno que RegisterForChampionshipUseCase.
 */
public class ChangeRegistrationCategoryUseCase implements UseCase<ChangeRegistrationCategoryCommand, Void> {

    private final RegistrationRepository registrationRepository;
    private final ChampionshipRepository championshipRepository;
    private final CategoryRepository     categoryRepository;
    private final UserRepository         userRepository;

    public ChangeRegistrationCategoryUseCase(
            RegistrationRepository registrationRepository,
            ChampionshipRepository championshipRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.registrationRepository = registrationRepository;
        this.championshipRepository = championshipRepository;
        this.categoryRepository     = categoryRepository;
        this.userRepository         = userRepository;
    }

    @Override
    public Void execute(ChangeRegistrationCategoryCommand cmd) {
        Registration registration = registrationRepository.findById(cmd.registrationId())
                .orElseThrow(() -> new NotFoundException(cmd.registrationId()));

        Championship championship = championshipRepository.findById(registration.getChampionshipId())
                .orElseThrow(() -> new RegisterForChampionshipUseCase.ChampionshipNotFoundException(
                        registration.getChampionshipId().value()));

        if (!championship.hasCategory(cmd.newCategoryId())) {
            throw new RegisterForChampionshipUseCase.CategoryNotInChampionshipException(
                    cmd.newCategoryId(), championship.getId().value());
        }

        Category newCategory = categoryRepository.findById(CategoryId.of(cmd.newCategoryId()))
                .orElseThrow(() -> new RegisterForChampionshipUseCase.CategoryNotInChampionshipException(
                        cmd.newCategoryId(), championship.getId().value()));

        if (newCategory.getAgeCategory() != null) {
            User user = userRepository.findById(registration.getUserId())
                    .orElseThrow(() -> new RegisterForChampionshipUseCase.UserNotFoundException(
                            registration.getUserId().value()));
            if (!newCategory.getAgeCategory().acceptsBirthDateForCompetitionYear(
                    user.getBirthDate(), championship.getEventDate().getYear())) {
                throw new RegisterForChampionshipUseCase.AgeCategoryNotEligibleException(
                        newCategory.getAgeCategory().getDisplayName(), championship.getEventDate().getYear());
            }
        }

        List<Registration> others = registrationRepository
                .findByUserAndChampionship(registration.getUserId(), registration.getChampionshipId())
                .stream()
                .filter(r -> !r.getId().equals(registration.getId()))
                .toList();

        for (Registration other : others) {
            if (other.getCategoryId().equals(cmd.newCategoryId())) {
                throw new RegisterForChampionshipUseCase.DuplicateRegistrationException(
                        registration.getUserId().value(), championship.getId().value());
            }
        }

        Shift requestedShift = newCategory.getShift();
        List<Category> sameShiftCategories = new ArrayList<>();
        for (Registration other : others) {
            Optional<Category> catOpt = categoryRepository.findById(CategoryId.of(other.getCategoryId()));
            if (catOpt.isPresent() && catOpt.get().getShift() == requestedShift) {
                sameShiftCategories.add(catOpt.get());
            }
        }

        if (!ChampionshipCategorySelectionPolicy.canAdd(sameShiftCategories, newCategory)) {
            throw new RegisterForChampionshipUseCase.SameShiftConflictException(requestedShift);
        }

        registration.changeCategory(cmd.newCategoryId());
        registrationRepository.save(registration);
        return null;
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Inscripción no encontrada: " + id);
        }
    }
}
