package es.aelb.backendweb.domain.pricing;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import es.aelb.backendweb.domain.shared.exception.DomainException;

import java.math.BigDecimal;
import java.util.*;

/** Política WAF de precios de inscripción, independiente de la forma de pago. */
public final class ChampionshipRegistrationPriceCalculator {

    public BigDecimal calculate(List<Category> categories, List<ChampionshipAgeCategoryPrice> configuredPrices) {
        if (categories.stream().anyMatch(category -> category.getAgeCategory() == null)) throw new MissingAgeCategoryException();
        Map<CompetitionAgeCategory, Long> armsByCategory = categories.stream()
                .map(Category::getAgeCategory)
                .collect(java.util.stream.Collectors.groupingBy(category -> category, () -> new EnumMap<>(CompetitionAgeCategory.class), java.util.stream.Collectors.counting()));

        Map<CompetitionAgeCategory, ChampionshipAgeCategoryPrice> prices = new EnumMap<>(CompetitionAgeCategory.class);
        configuredPrices.forEach(price -> prices.put(price.ageCategory(), price));
        for (CompetitionAgeCategory category : armsByCategory.keySet()) {
            prices.putIfAbsent(category, ChampionshipAgeCategoryPrice.defaultFor(category));
        }

        Optional<CompetitionAgeCategory> anchor = armsByCategory.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing((CompetitionAgeCategory category) -> category != CompetitionAgeCategory.SENIOR)
                        .thenComparing(Enum::name))
                .findFirst();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<CompetitionAgeCategory, Long> entry : armsByCategory.entrySet()) {
            ChampionshipAgeCategoryPrice price = prices.get(entry.getKey());
            if (anchor.isPresent() && armsByCategory.size() > 1 && entry.getKey() != anchor.get()) {
                total = total.add(price.combinationPrice());
            } else {
                total = total.add(price.pricePerArm().multiply(BigDecimal.valueOf(entry.getValue())));
            }
        }
        return total;
    }

    public static final class MissingAgeCategoryException extends DomainException {
        public MissingAgeCategoryException() { super("Todas las categorías del campeonato deben tener una categoría de edad WAF para calcular el precio."); }
    }
}
