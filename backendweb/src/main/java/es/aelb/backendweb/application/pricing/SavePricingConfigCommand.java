package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.domain.pricing.PricingEffectType;
import es.aelb.backendweb.domain.pricing.RuleConditionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Comando para crear o actualizar la configuración de precios de un campeonato.
 *
 * Si ya existe configuración para el campeonato, se reemplaza completamente.
 */
public record SavePricingConfigCommand(
        String championshipId,
        int maxCategoriesPerArm,
        int maxTotalEntries,
        String currency,
        List<RuleData> rules,
        List<RestrictionData> categoryRestrictions
) {
    public record RuleData(
            String name,
            String description,
            int priority,
            boolean active,
            List<ConditionData> conditions,
            EffectData effect
    ) {}

    public record ConditionData(
            RuleConditionType type,
            String value
    ) {}

    public record EffectData(
            PricingEffectType type,
            BigDecimal amount,
            String description
    ) {}

    public record RestrictionData(
            String categoryTypeId,
            Integer ageMin,
            Integer ageMax,
            Integer maxExperienceYears,
            Integer maxGoldMedals,
            Set<String> incompatibleWith,
            Set<String> requiresCombinationWith
    ) {}
}
