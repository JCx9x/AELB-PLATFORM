package es.aelb.backendweb.domain.category;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryTest {

    @Test
    void generatesTheTitleFromTheSelectedAttributes() {
        Category category = Category.create(
                Gender.MALE, ArmSide.RIGHT, new BigDecimal("80.00"),
                CompetitionAgeCategory.SENIOR.name(), CompetitionAgeCategory.SENIOR, Shift.MORNING
        );

        assertEquals("Senior Masculino Derecho -80kg", category.getName());
        assertEquals(Shift.AFTERNOON, category.getShift());
    }

    @Test
    void regeneratesTheTitleWhenAttributesChange() {
        Category category = Category.create(
                Gender.FEMALE, ArmSide.LEFT, null,
                CompetitionAgeCategory.AMATEUR.name(), CompetitionAgeCategory.AMATEUR, Shift.MORNING
        );

        category.update(Gender.OPEN, ArmSide.BOTH, new BigDecimal("70.5"),
                CompetitionAgeCategory.JUNIOR.name(), CompetitionAgeCategory.JUNIOR, Shift.AFTERNOON);

        assertEquals("Junior Open Ambos brazos -70.5kg", category.getName());
        assertEquals(Shift.MORNING, category.getShift());
    }
}
