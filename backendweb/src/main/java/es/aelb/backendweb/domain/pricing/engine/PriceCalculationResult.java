package es.aelb.backendweb.domain.pricing.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado del motor de precios.
 *
 * Si `valid` es false, el atleta no puede inscribirse con la combinación indicada
 * y `validationErrors` contiene el motivo.
 */
public final class PriceCalculationResult {

    private final BigDecimal         total;
    private final String             currency;
    private final boolean            valid;
    private final List<String>       validationErrors;
    private final List<PriceLineItem> lineItems;

    private PriceCalculationResult(
            BigDecimal total,
            String currency,
            boolean valid,
            List<String> validationErrors,
            List<PriceLineItem> lineItems
    ) {
        this.total            = total;
        this.currency         = currency;
        this.valid            = valid;
        this.validationErrors = List.copyOf(validationErrors);
        this.lineItems        = List.copyOf(lineItems);
    }

    public static PriceCalculationResult valid(
            BigDecimal total,
            String currency,
            List<PriceLineItem> lineItems
    ) {
        return new PriceCalculationResult(total, currency, true, List.of(), lineItems);
    }

    public static PriceCalculationResult invalid(List<String> errors) {
        return new PriceCalculationResult(BigDecimal.ZERO, "EUR", false, errors, List.of());
    }

    public BigDecimal          getTotal()            { return total; }
    public String              getCurrency()         { return currency; }
    public boolean             isValid()             { return valid; }
    public List<String>        getValidationErrors() { return validationErrors; }
    public List<PriceLineItem> getLineItems()        { return lineItems; }
}
