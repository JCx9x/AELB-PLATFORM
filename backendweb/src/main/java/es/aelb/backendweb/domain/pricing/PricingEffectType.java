package es.aelb.backendweb.domain.pricing;

/**
 * Tipos de efecto que una regla produce sobre el precio.
 *
 * BASE_PER_ARM                   : cobra `amount` por cada brazo distinto en el basket
 * SUPPLEMENT_PER_EXTRA_CATEGORY  : cobra `amount` por cada tipo de categoría adicional (más allá del primero)
 * BUNDLE_FIXED_TOTAL             : sobreescribe el total con `amount` (combos con precio especial)
 * SUPPLEMENT_FLAT                : añade `amount` fijo al total base (suplemento plano)
 * DISCOUNT_FIXED                 : descuento fijo en euros (se aplica después de sumas; floor a 0€)
 * DISCOUNT_PERCENT               : descuento porcentual sobre el subtotal acumulado hasta ese punto
 */
public enum PricingEffectType {
    BASE_PER_ARM,
    SUPPLEMENT_PER_EXTRA_CATEGORY,
    BUNDLE_FIXED_TOTAL,
    SUPPLEMENT_FLAT,
    DISCOUNT_FIXED,
    DISCOUNT_PERCENT
}
