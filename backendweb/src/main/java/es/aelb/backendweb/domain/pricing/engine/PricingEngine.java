package es.aelb.backendweb.domain.pricing.engine;

import es.aelb.backendweb.domain.category.ArmSide;
import es.aelb.backendweb.domain.pricing.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Motor de cálculo de precios. Servicio de dominio puro (sin dependencias de infraestructura).
 *
 * ─────────────────── ALGORITMO ───────────────────
 *
 * FASE 1 — VALIDACIÓN
 *   a) Restricciones del basket (max categorías/brazo, max total).
 *   b) Elegibilidad por CategoryRestriction (edad, experiencia, oros, incompatibilidades).
 *   Si hay errores → devuelve resultado inválido y para.
 *
 * FASE 2 — CÁLCULO
 *   a) BUNDLE override: si alguna regla BUNDLE_FIXED_TOTAL aplica (prioridad menor = mayor),
 *      su importe es el total final. No se evalúa nada más.
 *   b) Cálculo estándar (por prioridad ascendente):
 *      1. BASE_PER_ARM              → importe × brazos distintos
 *      2. SUPPLEMENT_PER_EXTRA_CAT → importe × (tipos_cat - 1)
 *      3. SUPPLEMENT_FLAT          → importe fijo si condiciones OK
 *      4. DISCOUNT_FIXED           → resta importe fijo (floor a 0€)
 *      5. DISCOUNT_PERCENT         → resta X% del subtotal en ese momento
 */
public class PricingEngine {

    public PriceCalculationResult calculate(
            PricingConfig config,
            RegistrationBasket basket,
            AthleteContext athlete
    ) {
        List<String> errors = new ArrayList<>();

        validateBasketConstraints(config, basket, errors);
        validateEligibility(config, basket, athlete, errors);

        if (!errors.isEmpty()) {
            return PriceCalculationResult.invalid(errors);
        }

        // ── Fase 2a: Bundle override ──────────────────────────────────────
        Optional<PricingRule> bundle = config.getActiveRules().stream()
                .filter(r -> r.getEffect().type() == PricingEffectType.BUNDLE_FIXED_TOTAL)
                .filter(r -> allConditionsMatch(r, basket, athlete))
                .min(Comparator.comparingInt(PricingRule::getPriority));

        if (bundle.isPresent()) {
            PricingRule r = bundle.get();
            List<PriceLineItem> items = List.of(
                    PriceLineItem.of(r.getEffect().description(), r.getEffect().amount(), r.getId())
            );
            return PriceCalculationResult.valid(r.getEffect().amount(), config.getCurrency(), items);
        }

        // ── Fase 2b: Cálculo estándar ─────────────────────────────────────
        List<PriceLineItem> lineItems = new ArrayList<>();
        Set<ArmSide>        arms      = basket.getDistinctArmSides();
        int                 extraCats = Math.max(0, basket.getDistinctCategoryTypes().size() - 1);

        // Pasada 1: adiciones (BASE + SUPPLEMENTS)
        for (PricingRule rule : config.getActiveRulesSorted()) {
            if (!allConditionsMatch(rule, basket, athlete)) continue;

            switch (rule.getEffect().type()) {

                case BASE_PER_ARM -> {
                    for (ArmSide arm : arms) {
                        lineItems.add(PriceLineItem.of(
                                rule.getEffect().description() + " - brazo " + armLabel(arm),
                                rule.getEffect().amount(),
                                rule.getId()
                        ));
                    }
                }

                case SUPPLEMENT_PER_EXTRA_CATEGORY -> {
                    for (int i = 0; i < extraCats; i++) {
                        lineItems.add(PriceLineItem.of(
                                rule.getEffect().description(),
                                rule.getEffect().amount(),
                                rule.getId()
                        ));
                    }
                }

                case SUPPLEMENT_FLAT ->
                        lineItems.add(PriceLineItem.of(
                                rule.getEffect().description(),
                                rule.getEffect().amount(),
                                rule.getId()
                        ));

                default -> { /* BUNDLE y DISCOUNTs se gestionan antes/después */ }
            }
        }

        // Pasada 2: descuentos (sobre el subtotal calculado)
        BigDecimal subtotal = lineItems.stream()
                .map(PriceLineItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (PricingRule rule : config.getActiveRulesSorted()) {
            if (!allConditionsMatch(rule, basket, athlete)) continue;

            switch (rule.getEffect().type()) {

                case DISCOUNT_FIXED -> {
                    if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discount = rule.getEffect().amount().min(subtotal); // floor a 0€
                        lineItems.add(PriceLineItem.of(
                                rule.getEffect().description(),
                                discount.negate(),
                                rule.getId()
                        ));
                        subtotal = subtotal.subtract(discount);
                    }
                }

                case DISCOUNT_PERCENT -> {
                    if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal pct      = rule.getEffect().amount();
                        BigDecimal discount = subtotal
                                .multiply(pct)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        lineItems.add(PriceLineItem.of(
                                rule.getEffect().description() + " (-" + pct.stripTrailingZeros().toPlainString() + "%)",
                                discount.negate(),
                                rule.getId()
                        ));
                        subtotal = subtotal.subtract(discount);
                    }
                }

                default -> {}
            }
        }

        BigDecimal total = lineItems.stream()
                .map(PriceLineItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO); // nunca negativo

        return PriceCalculationResult.valid(total, config.getCurrency(), lineItems);
    }

    // ── Validaciones ─────────────────────────────────────────────────────

    private void validateBasketConstraints(PricingConfig config, RegistrationBasket basket, List<String> errors) {
        if (basket.size() > config.getMaxTotalEntries()) {
            errors.add("El basket supera el máximo de " + config.getMaxTotalEntries() + " inscripciones permitidas.");
        }
        for (ArmSide arm : ArmSide.values()) {
            int count = basket.entriesForArm(arm).size();
            if (count > config.getMaxCategoriesPerArm()) {
                errors.add("El brazo " + armLabel(arm) + " tiene " + count
                        + " categorías. El máximo es " + config.getMaxCategoriesPerArm() + ".");
            }
        }
    }

    private void validateEligibility(
            PricingConfig config,
            RegistrationBasket basket,
            AthleteContext athlete,
            List<String> errors
    ) {
        Set<String> basketTypes = basket.getDistinctCategoryTypes();

        for (CategoryRestriction r : config.getCategoryRestrictions()) {
            if (!basketTypes.contains(r.getCategoryTypeId())) continue;
            String cat = r.getCategoryTypeId();

            if (r.getAgeMin() != null && athlete.age() < r.getAgeMin())
                errors.add("La categoría " + cat + " requiere edad mínima de " + r.getAgeMin() + " años.");

            if (r.getAgeMax() != null && athlete.age() > r.getAgeMax())
                errors.add("La categoría " + cat + " es solo hasta " + r.getAgeMax() + " años.");

            if (r.getMaxExperienceYears() != null && athlete.experienceYears() > r.getMaxExperienceYears())
                errors.add("La categoría " + cat + " requiere menos de "
                        + (r.getMaxExperienceYears() + 1) + " años de experiencia.");

            if (r.getMaxGoldMedals() != null && athlete.goldMedals() > r.getMaxGoldMedals())
                errors.add("La categoría " + cat + " no está disponible con "
                        + (r.getMaxGoldMedals() + 1) + " o más oros.");

            for (String incompatible : r.getIncompatibleWith()) {
                if (basketTypes.contains(incompatible))
                    errors.add("La categoría " + cat + " es incompatible con " + incompatible + ".");
            }

            if (!r.getRequiresCombinationWith().isEmpty()) {
                boolean ok = r.getRequiresCombinationWith().stream().anyMatch(basketTypes::contains);
                if (!ok)
                    errors.add("La categoría " + cat + " debe combinarse con alguna de: "
                            + String.join(", ", r.getRequiresCombinationWith()) + ".");
            }
        }
    }

    // ── Evaluación de condiciones ─────────────────────────────────────────

    private boolean allConditionsMatch(PricingRule rule, RegistrationBasket basket, AthleteContext athlete) {
        for (RuleCondition c : rule.getConditions()) {
            if (!conditionMatches(c, basket, athlete)) return false;
        }
        return true;
    }

    private boolean conditionMatches(RuleCondition condition, RegistrationBasket basket, AthleteContext athlete) {
        Set<String> types = basket.getDistinctCategoryTypes();
        int         armCount = basket.getDistinctArmSides().size();

        return switch (condition.type()) {
            case BASKET_CONTAINS_ONLY_TYPES -> {
                Set<String> allowed = splitToSet(condition.value());
                yield types.stream().allMatch(allowed::contains);
            }
            case BASKET_CONTAINS_ALL_TYPES -> {
                Set<String> required = splitToSet(condition.value());
                yield types.containsAll(required);
            }
            case BASKET_ARM_COUNT_IS       -> armCount == intVal(condition.value());
            case BASKET_ARM_COUNT_GTE      -> armCount >= intVal(condition.value());
            case BASKET_CATEGORY_COUNT_IS  -> types.size() == intVal(condition.value());
            case AGE_MAX                   -> athlete.age() <= intVal(condition.value());
            case AGE_MIN                   -> athlete.age() >= intVal(condition.value());
            case EXPERIENCE_YEARS_MAX      -> athlete.experienceYears() <= intVal(condition.value());
            case GOLD_MEDALS_MAX           -> athlete.goldMedals() <= intVal(condition.value());
        };
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private static Set<String> splitToSet(String csv) {
        Set<String> result = new HashSet<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    private static int intVal(String value) {
        return Integer.parseInt(value.trim());
    }

    private static String armLabel(ArmSide arm) {
        return switch (arm) {
            case RIGHT -> "derecho";
            case LEFT  -> "izquierdo";
            case BOTH  -> "ambos";
        };
    }
}
