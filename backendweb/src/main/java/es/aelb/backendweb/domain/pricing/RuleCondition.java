package es.aelb.backendweb.domain.pricing;

/**
 * Condición que debe cumplirse para que una regla se active.
 *
 * `value` es una cadena serializada según el tipo:
 *   - AGE_MAX / AGE_MIN / EXPERIENCE_YEARS_MAX / GOLD_MEDALS_MAX → número entero ("15", "2")
 *   - BASKET_CONTAINS_ONLY_TYPES / BASKET_CONTAINS_ALL_TYPES     → IDs de tipo separados por coma ("JUNIOR_18,SENIOR")
 */
public record RuleCondition(RuleConditionType type, String value) {

    public RuleCondition {
        if (type == null) throw new IllegalArgumentException("El tipo de condición no puede ser nulo");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("El valor de la condición no puede estar vacío");
    }

    public static RuleCondition of(RuleConditionType type, String value) {
        return new RuleCondition(type, value);
    }
}
