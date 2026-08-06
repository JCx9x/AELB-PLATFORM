package es.aelb.backendweb.domain.pricing;

import es.aelb.backendweb.domain.category.CompetitionAgeCategory;

import java.math.BigDecimal;

/** Precio de una categoría WAF dentro de un campeonato. */
public record ChampionshipAgeCategoryPrice(
        CompetitionAgeCategory ageCategory,
        BigDecimal pricePerArm,
        BigDecimal combinationPrice
) {
    public ChampionshipAgeCategoryPrice {
        if (ageCategory == null) throw new IllegalArgumentException("La categoría de edad es obligatoria");
        if (pricePerArm == null || pricePerArm.signum() < 0) throw new IllegalArgumentException("El precio por brazo no puede ser negativo");
        if (combinationPrice == null || combinationPrice.signum() < 0) throw new IllegalArgumentException("El suplemento de combinación no puede ser negativo");
    }

    public static ChampionshipAgeCategoryPrice defaultFor(CompetitionAgeCategory category) {
        return new ChampionshipAgeCategoryPrice(category, new BigDecimal("20.00"), new BigDecimal("10.00"));
    }
}
