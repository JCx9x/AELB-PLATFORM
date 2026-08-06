package es.aelb.backendweb.infrastructure.web.dto.response;
import es.aelb.backendweb.domain.quota.AnnualQuotaPrice;
import java.math.BigDecimal;
public record AnnualQuotaPriceResponse(String ageCategory, String label, Integer minimumAge, Integer maximumAge, BigDecimal amount) {
    public static AnnualQuotaPriceResponse from(AnnualQuotaPrice p) { return new AnnualQuotaPriceResponse(p.getAgeCategory().name(), p.getAgeCategory().getLabel(), p.getAgeCategory().getMinimumAge(), p.getAgeCategory().getMaximumAge(), p.getAmount()); }
}
