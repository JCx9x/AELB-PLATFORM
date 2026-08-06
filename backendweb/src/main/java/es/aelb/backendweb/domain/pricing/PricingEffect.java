package es.aelb.backendweb.domain.pricing;

import java.math.BigDecimal;

/**
 * Efecto económico que produce una regla cuando todas sus condiciones se cumplen.
 */
public record PricingEffect(PricingEffectType type, BigDecimal amount, String description) {

    public PricingEffect {
        if (type == null) throw new IllegalArgumentException("El tipo de efecto no puede ser nulo");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("El importe del efecto no puede ser negativo");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("La descripción del efecto no puede estar vacía");
    }

    public static PricingEffect of(PricingEffectType type, BigDecimal amount, String description) {
        return new PricingEffect(type, amount, description);
    }
}
