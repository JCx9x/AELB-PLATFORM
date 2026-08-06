package es.aelb.backendweb.infrastructure.web.dto.request;

import es.aelb.backendweb.domain.pricing.PricingEffectType;
import es.aelb.backendweb.domain.pricing.RuleConditionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record PricingConfigRequest(
        @NotNull @Positive int maxCategoriesPerArm,
        @NotNull @Positive int maxTotalEntries,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @Valid List<RuleRequest> rules,
        @NotNull @Valid List<RestrictionRequest> categoryRestrictions
) {
    public record RuleRequest(
            @NotBlank String name,
            String description,
            int priority,
            boolean active,
            @NotNull @Valid List<ConditionRequest> conditions,
            @NotNull @Valid EffectRequest effect
    ) {}

    public record ConditionRequest(
            @NotNull RuleConditionType type,
            @NotBlank String value
    ) {}

    public record EffectRequest(
            @NotNull PricingEffectType type,
            @NotNull @DecimalMin("0.00") BigDecimal amount,
            @NotBlank String description
    ) {}

    public record RestrictionRequest(
            @NotBlank String categoryTypeId,
            @Min(0) Integer ageMin,
            @Min(0) Integer ageMax,
            @Min(0) Integer maxExperienceYears,
            @Min(0) Integer maxGoldMedals,
            Set<String> incompatibleWith,
            Set<String> requiresCombinationWith
    ) {}
}
