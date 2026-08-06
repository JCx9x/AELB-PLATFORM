package es.aelb.backendweb.domain.pricing.checkout;

import java.math.BigDecimal;

/**
 * Línea de concepto en el checkout: lo que aparecerá en el desglose de pago.
 */
public record CheckoutLineItem(
        String     concept,
        BigDecimal amount,
        String     ruleId
) {
    public static CheckoutLineItem of(String concept, BigDecimal amount, String ruleId) {
        return new CheckoutLineItem(concept, amount, ruleId);
    }
}
