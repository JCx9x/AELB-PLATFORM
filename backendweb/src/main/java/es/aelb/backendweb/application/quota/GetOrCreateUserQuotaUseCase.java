package es.aelb.backendweb.application.quota;
import es.aelb.backendweb.domain.quota.*;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import java.time.LocalDate;
import java.time.Period;
public class GetOrCreateUserQuotaUseCase {
    private final QuotaRepository quotaRepository;
    private final AnnualQuotaPriceRepository priceRepository;
    private final UserRepository userRepository;
    public GetOrCreateUserQuotaUseCase(QuotaRepository quotaRepository, AnnualQuotaPriceRepository priceRepository, UserRepository userRepository) { this.quotaRepository = quotaRepository; this.priceRepository = priceRepository; this.userRepository = userRepository; }
    public Quota execute(String userId, int year) {
        UserId id = UserId.of(userId);
        return quotaRepository.findByUserAndYear(id, year).orElseGet(() -> create(id, year));
    }
    private Quota create(UserId userId, int year) {
        User user = userRepository.findById(userId).orElseThrow(() -> new User.NotFoundException(userId.value()));
        if (user.getBirthDate() == null) throw new BirthDateRequiredException();
        int age = Period.between(user.getBirthDate(), LocalDate.of(year, 12, 31)).getYears();
        if (age < 0) throw new BirthDateRequiredException();
        AgeCategory category = AgeCategory.fromAge(age);
        AnnualQuotaPrice price = priceRepository.findByYearAndAgeCategory(year, category).orElseThrow(() -> new AnnualQuotaPrice.NotConfiguredException(year, category));
        Quota quota = Quota.create(userId, year, category, price.getAmount());
        quotaRepository.save(quota);
        return quota;
    }
    public static final class BirthDateRequiredException extends DomainException { public BirthDateRequiredException() { super("Debes indicar tu fecha de nacimiento antes de generar una cuota"); } }
}
