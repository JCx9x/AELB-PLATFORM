package es.aelb.backendweb.application.quota;
import es.aelb.backendweb.domain.quota.*;
import java.math.BigDecimal;
import java.util.*;
public class SaveAnnualQuotaPricesUseCase {
    private final AnnualQuotaPriceRepository repository;
    public SaveAnnualQuotaPricesUseCase(AnnualQuotaPriceRepository repository) { this.repository = repository; }
    public List<AnnualQuotaPrice> execute(int year, Map<AgeCategory, BigDecimal> amounts) {
        if (!amounts.keySet().containsAll(EnumSet.allOf(AgeCategory.class))) throw new IllegalArgumentException("Debes configurar el precio de todos los tramos de edad");
        List<AnnualQuotaPrice> prices = Arrays.stream(AgeCategory.values()).map(c -> new AnnualQuotaPrice(year, c, amounts.get(c))).toList();
        repository.saveAll(prices);
        return prices;
    }
}
