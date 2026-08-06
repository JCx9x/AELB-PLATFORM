package es.aelb.backendweb.domain.pricing;

import es.aelb.backendweb.domain.category.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChampionshipRegistrationPriceCalculatorTest {
    private final ChampionshipRegistrationPriceCalculator calculator = new ChampionshipRegistrationPriceCalculator();
    private final List<ChampionshipAgeCategoryPrice> prices = List.of(
            new ChampionshipAgeCategoryPrice(CompetitionAgeCategory.SENIOR, new BigDecimal("20"), new BigDecimal("10")),
            new ChampionshipAgeCategoryPrice(CompetitionAgeCategory.YOUTH, new BigDecimal("20"), new BigDecimal("10"))
    );

    @Test void chargesNormalPriceWhenThereIsOneArmInEachCategory() {
        assertEquals(new BigDecimal("40"), calculator.calculate(List.of(category(CompetitionAgeCategory.SENIOR, ArmSide.RIGHT), category(CompetitionAgeCategory.YOUTH, ArmSide.RIGHT)), prices));
    }

    @Test void chargesFixedSupplementWhenSeniorHasTwoArms() {
        assertEquals(new BigDecimal("50"), calculator.calculate(List.of(category(CompetitionAgeCategory.SENIOR, ArmSide.RIGHT), category(CompetitionAgeCategory.SENIOR, ArmSide.LEFT), category(CompetitionAgeCategory.YOUTH, ArmSide.RIGHT)), prices));
    }

    @Test void chargesFixedSupplementWhenTheOtherCategoryHasTwoArms() {
        assertEquals(new BigDecimal("50"), calculator.calculate(List.of(category(CompetitionAgeCategory.SENIOR, ArmSide.RIGHT), category(CompetitionAgeCategory.YOUTH, ArmSide.RIGHT), category(CompetitionAgeCategory.YOUTH, ArmSide.LEFT)), prices));
    }

    private static Category category(CompetitionAgeCategory ageCategory, ArmSide arm) {
        return Category.create(Gender.OPEN, arm, null, ageCategory.name(), ageCategory, ageCategory.getShift());
    }
}
