package es.aelb.backendweb.infrastructure.web.dto.request;

import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ChampionshipAgeCategoryPricesRequest(@NotNull @Valid List<PriceRequest> prices) {
    public record PriceRequest(
            @NotNull CompetitionAgeCategory ageCategory,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerArm,
            @NotNull @DecimalMin("0.00") BigDecimal combinationPrice
    ) {}
}
