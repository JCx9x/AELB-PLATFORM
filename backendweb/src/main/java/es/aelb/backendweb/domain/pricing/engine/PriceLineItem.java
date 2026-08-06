package es.aelb.backendweb.domain.pricing.engine;

import java.math.BigDecimal;

/**
 * Línea de desglose en el resultado del cálculo de precio.
 * Cada línea representa un concepto cobrable.
 */
public record PriceLineItem(String concept, BigDecimal amount, String ruleId) {

    public static PriceLineItem of(String concept, BigDecimal amount, String ruleId) {
        return new PriceLineItem(concept, amount, ruleId);
    }
}
