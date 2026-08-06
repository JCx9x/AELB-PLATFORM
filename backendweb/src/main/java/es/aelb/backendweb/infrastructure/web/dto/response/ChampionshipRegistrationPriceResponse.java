package es.aelb.backendweb.infrastructure.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ChampionshipRegistrationPriceResponse(
        BigDecimal total,
        String currency,
        List<String> categoryIds,
        List<Item> items
) {
    public record Item(String categoryId, String name, String armSide, BigDecimal weightLimit) {}
}
