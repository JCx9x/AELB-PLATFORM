package es.aelb.backendweb.domain.registration;

import es.aelb.backendweb.domain.category.ArmSide;
import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import es.aelb.backendweb.domain.category.Gender;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChampionshipCategorySelectionPolicyTest {

    @Test
    void acceptsOnlyTheRemainingArmOfTheSameMorningCategory() {
        Category amateurLeft = category(CompetitionAgeCategory.AMATEUR, ArmSide.LEFT, "70");
        Category amateurRight = category(CompetitionAgeCategory.AMATEUR, ArmSide.RIGHT, "70");
        Category youthRight = category(CompetitionAgeCategory.YOUTH, ArmSide.RIGHT, "70");

        assertTrue(ChampionshipCategorySelectionPolicy.canAdd(List.of(amateurLeft), amateurRight));
        assertFalse(ChampionshipCategorySelectionPolicy.canAdd(List.of(amateurLeft), youthRight));
    }

    @Test
    void rejectsAnotherWeightOrArmInTheSeniorAfternoonShift() {
        Category seniorLeft70 = category(CompetitionAgeCategory.SENIOR, ArmSide.LEFT, "70");
        Category seniorRight70 = category(CompetitionAgeCategory.SENIOR, ArmSide.RIGHT, "70");
        Category seniorRight80 = category(CompetitionAgeCategory.SENIOR, ArmSide.RIGHT, "80");

        assertTrue(ChampionshipCategorySelectionPolicy.canAdd(List.of(seniorLeft70), seniorRight70));
        assertFalse(ChampionshipCategorySelectionPolicy.canAdd(List.of(seniorLeft70), seniorRight80));
    }

    private static Category category(CompetitionAgeCategory ageCategory, ArmSide armSide, String weight) {
        return Category.create(Gender.MALE, armSide, new BigDecimal(weight), ageCategory.name(), ageCategory, ageCategory.getShift());
    }
}
