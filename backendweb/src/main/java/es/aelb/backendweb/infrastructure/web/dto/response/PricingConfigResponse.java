package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.pricing.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record PricingConfigResponse(
        String id,
        String championshipId,
        int maxCategoriesPerArm,
        int maxTotalEntries,
        String currency,
        boolean active,
        List<RuleResponse> rules,
        List<RestrictionResponse> categoryRestrictions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record RuleResponse(
            String id,
            String name,
            String description,
            int priority,
            boolean active,
            List<ConditionResponse> conditions,
            EffectResponse effect
    ) {}

    public record ConditionResponse(RuleConditionType type, String value) {}

    public record EffectResponse(PricingEffectType type, BigDecimal amount, String description) {}

    public record RestrictionResponse(
            String categoryTypeId,
            Integer ageMin,
            Integer ageMax,
            Integer maxExperienceYears,
            Integer maxGoldMedals,
            Set<String> incompatibleWith,
            Set<String> requiresCombinationWith
    ) {}

    public static PricingConfigResponse from(PricingConfig config) {
        List<RuleResponse> rules = config.getRules().stream()
                .map(r -> new RuleResponse(
                        r.getId(),
                        r.getName(),
                        r.getDescription(),
                        r.getPriority(),
                        r.isActive(),
                        r.getConditions().stream()
                                .map(c -> new ConditionResponse(c.type(), c.value()))
                                .toList(),
                        new EffectResponse(r.getEffect().type(), r.getEffect().amount(), r.getEffect().description())
                ))
                .toList();

        List<RestrictionResponse> restrictions = config.getCategoryRestrictions().stream()
                .map(r -> new RestrictionResponse(
                        r.getCategoryTypeId(),
                        r.getAgeMin(),
                        r.getAgeMax(),
                        r.getMaxExperienceYears(),
                        r.getMaxGoldMedals(),
                        r.getIncompatibleWith(),
                        r.getRequiresCombinationWith()
                ))
                .toList();

        return new PricingConfigResponse(
                config.getId().value(),
                config.getChampionshipId().value(),
                config.getMaxCategoriesPerArm(),
                config.getMaxTotalEntries(),
                config.getCurrency(),
                config.isActive(),
                rules,
                restrictions,
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
