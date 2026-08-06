package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.pricing.engine.PriceCalculationResult;
import es.aelb.backendweb.domain.pricing.engine.PriceLineItem;

import java.math.BigDecimal;
import java.util.List;

public record PriceCalculationResponse(
        BigDecimal total,
        String currency,
        boolean valid,
        List<String> validationErrors,
        List<LineItemResponse> lineItems
) {
    public record LineItemResponse(String concept, BigDecimal amount) {}

    public static PriceCalculationResponse from(PriceCalculationResult result) {
        List<LineItemResponse> items = result.getLineItems().stream()
                .map(li -> new LineItemResponse(li.concept(), li.amount()))
                .toList();
        return new PriceCalculationResponse(
                result.getTotal(),
                result.getCurrency(),
                result.isValid(),
                result.getValidationErrors(),
                items
        );
    }
}
