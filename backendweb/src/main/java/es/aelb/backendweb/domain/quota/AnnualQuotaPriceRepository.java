package es.aelb.backendweb.domain.quota;

import java.util.List;
import java.util.Optional;

public interface AnnualQuotaPriceRepository {
    void saveAll(List<AnnualQuotaPrice> prices);
    Optional<AnnualQuotaPrice> findByYearAndAgeCategory(int year, AgeCategory ageCategory);
    List<AnnualQuotaPrice> findByYear(int year);
}
