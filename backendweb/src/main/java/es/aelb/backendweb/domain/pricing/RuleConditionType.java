package es.aelb.backendweb.domain.pricing;

/**
 * Tipos de condición que una regla puede evaluar sobre el basket del atleta.
 *
 * BASKET_CONTAINS_ONLY_TYPES : el basket contiene SOLO los tipos indicados (separados por coma)
 * BASKET_CONTAINS_ALL_TYPES  : el basket contiene TODOS los tipos indicados (puede tener más)
 * BASKET_ARM_COUNT_IS        : el número de brazos distintos en el basket == valor
 * BASKET_ARM_COUNT_GTE       : número de brazos distintos >= valor
 * BASKET_CATEGORY_COUNT_IS   : número de tipos de categoría distintos == valor
 * AGE_MAX                    : edad del atleta <= valor
 * AGE_MIN                    : edad del atleta >= valor
 * EXPERIENCE_YEARS_MAX       : años compitiendo del atleta <= valor
 * GOLD_MEDALS_MAX            : medallas de oro <= valor
 */
public enum RuleConditionType {
    BASKET_CONTAINS_ONLY_TYPES,
    BASKET_CONTAINS_ALL_TYPES,
    BASKET_ARM_COUNT_IS,
    BASKET_ARM_COUNT_GTE,
    BASKET_CATEGORY_COUNT_IS,
    AGE_MAX,
    AGE_MIN,
    EXPERIENCE_YEARS_MAX,
    GOLD_MEDALS_MAX
}
