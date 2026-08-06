package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.pricing.ChampionshipAgeCategoryPrice;

import java.math.BigDecimal;

public record ChampionshipAgeCategoryPriceResponse(String ageCategory, BigDecimal pricePerArm, BigDecimal combinationPrice) {
    public static ChampionshipAgeCategoryPriceResponse from(ChampionshipAgeCategoryPrice price) {
        return new ChampionshipAgeCategoryPriceResponse(price.ageCategory().name(), price.pricePerArm(), price.combinationPrice());
    }
}
