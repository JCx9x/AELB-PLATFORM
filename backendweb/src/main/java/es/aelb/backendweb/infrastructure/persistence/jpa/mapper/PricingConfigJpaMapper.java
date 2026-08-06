package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.*;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.CategoryRestrictionJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.PricingConfigJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.PricingRuleJpaEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class PricingConfigJpaMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PricingConfigJpaMapper() {}

    // ── Domain → JPA ──────────────────────────────────────────────────────

    public static PricingConfigJpaEntity toJpa(PricingConfig domain) {
        PricingConfigJpaEntity entity = new PricingConfigJpaEntity();
        entity.setId(domain.getId().value());
        entity.setChampionshipId(domain.getChampionshipId().value());
        entity.setMaxCategoriesPerArm(domain.getMaxCategoriesPerArm());
        entity.setMaxTotalEntries(domain.getMaxTotalEntries());
        entity.setCurrency(domain.getCurrency());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        // Reglas
        entity.getRules().clear();
        for (PricingRule rule : domain.getRules()) {
            PricingRuleJpaEntity ruleEntity = toJpa(rule, entity);
            entity.getRules().add(ruleEntity);
        }

        // Restricciones
        entity.getCategoryRestrictions().clear();
        for (CategoryRestriction restriction : domain.getCategoryRestrictions()) {
            CategoryRestrictionJpaEntity re = toJpa(restriction, entity);
            entity.getCategoryRestrictions().add(re);
        }

        return entity;
    }

    private static PricingRuleJpaEntity toJpa(PricingRule rule, PricingConfigJpaEntity parent) {
        PricingRuleJpaEntity entity = new PricingRuleJpaEntity();
        entity.setId(rule.getId());
        entity.setPricingConfig(parent);
        entity.setName(rule.getName());
        entity.setDescription(rule.getDescription());
        entity.setPriority(rule.getPriority());
        entity.setActive(rule.isActive());
        entity.setConditionsJson(serializeConditions(rule.getConditions()));
        entity.setEffectJson(serializeEffect(rule.getEffect()));
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private static CategoryRestrictionJpaEntity toJpa(CategoryRestriction r, PricingConfigJpaEntity parent) {
        CategoryRestrictionJpaEntity entity = new CategoryRestrictionJpaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPricingConfig(parent);
        entity.setCategoryTypeId(r.getCategoryTypeId());
        entity.setAgeMin(r.getAgeMin());
        entity.setAgeMax(r.getAgeMax());
        entity.setMaxExperienceYears(r.getMaxExperienceYears());
        entity.setMaxGoldMedals(r.getMaxGoldMedals());
        entity.setIncompatibleWith(r.getIncompatibleWith().isEmpty() ? null : String.join(",", r.getIncompatibleWith()));
        entity.setRequiresCombination(r.getRequiresCombinationWith().isEmpty() ? null : String.join(",", r.getRequiresCombinationWith()));
        return entity;
    }

    // ── JPA → Domain ──────────────────────────────────────────────────────

    public static PricingConfig toDomain(PricingConfigJpaEntity entity) {
        List<PricingRule> rules = entity.getRules().stream()
                .map(PricingConfigJpaMapper::ruleToDomain)
                .toList();

        List<CategoryRestriction> restrictions = entity.getCategoryRestrictions().stream()
                .map(PricingConfigJpaMapper::restrictionToDomain)
                .toList();

        return PricingConfig.reconstitute(
                PricingConfigId.of(entity.getId()),
                ChampionshipId.of(entity.getChampionshipId()),
                entity.getMaxCategoriesPerArm(),
                entity.getMaxTotalEntries(),
                entity.getCurrency(),
                entity.isActive(),
                rules,
                restrictions,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static PricingRule ruleToDomain(PricingRuleJpaEntity entity) {
        return PricingRule.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPriority(),
                entity.isActive(),
                deserializeConditions(entity.getConditionsJson()),
                deserializeEffect(entity.getEffectJson())
        );
    }

    private static CategoryRestriction restrictionToDomain(CategoryRestrictionJpaEntity entity) {
        return new CategoryRestriction(
                entity.getCategoryTypeId(),
                entity.getAgeMin(),
                entity.getAgeMax(),
                entity.getMaxExperienceYears(),
                entity.getMaxGoldMedals(),
                csvToSet(entity.getIncompatibleWith()),
                csvToSet(entity.getRequiresCombination())
        );
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    private static String serializeConditions(List<RuleCondition> conditions) {
        try {
            List<Map<String, String>> list = conditions.stream()
                    .map(c -> Map.of("type", c.type().name(), "value", c.value()))
                    .toList();
            return MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando condiciones", e);
        }
    }

    private static List<RuleCondition> deserializeConditions(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptyList();
        try {
            List<Map<String, String>> list = MAPPER.readValue(json, new TypeReference<>() {});
            return list.stream()
                    .map(m -> RuleCondition.of(RuleConditionType.valueOf(m.get("type")), m.get("value")))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error deserializando condiciones: " + json, e);
        }
    }

    private static String serializeEffect(PricingEffect effect) {
        try {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("type",        effect.type().name());
            map.put("amount",      effect.amount().toPlainString());
            map.put("description", effect.description());
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando efecto", e);
        }
    }

    private static PricingEffect deserializeEffect(String json) {
        try {
            Map<String, String> map = MAPPER.readValue(json, new TypeReference<>() {});
            return PricingEffect.of(
                    PricingEffectType.valueOf(map.get("type")),
                    new BigDecimal(map.get("amount")),
                    map.get("description")
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error deserializando efecto: " + json, e);
        }
    }

    private static Set<String> csvToSet(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
