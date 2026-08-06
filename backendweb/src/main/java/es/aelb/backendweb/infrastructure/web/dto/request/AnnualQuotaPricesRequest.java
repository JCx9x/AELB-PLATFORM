package es.aelb.backendweb.infrastructure.web.dto.request;
import es.aelb.backendweb.domain.quota.AgeCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;
public record AnnualQuotaPricesRequest(@NotEmpty List<@Valid Price> prices) {
    public record Price(@NotNull AgeCategory ageCategory, @NotNull @DecimalMin("0.00") BigDecimal amount) {}
}
