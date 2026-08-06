package es.aelb.backendweb.domain.category;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompetitionAgeCategoryTest {

    @Test
    void definesOfficialCategoriesAndTheirShifts() {
        assertEquals(9, CompetitionAgeCategory.values().length);
        assertEquals(List.of(
                        "SUB_JUNIOR", "JUNIOR", "YOUTH", "AMATEUR", "SENIOR", "MASTER", "GRAND_MASTER",
                        "SENIOR_GRAND_MASTER", "SUPER_SENIOR_GRAND_MASTER"
                ),
                Arrays.stream(CompetitionAgeCategory.values()).map(Enum::name).toList());
        assertEquals(Shift.AFTERNOON, CompetitionAgeCategory.SENIOR.getShift());
        assertEquals(8, Arrays.stream(CompetitionAgeCategory.values())
                .filter(category -> category.getShift() == Shift.MORNING)
                .count());
    }

    @Test
    void acceptsAllInclusiveLimitedRanges() {
        assertRange(CompetitionAgeCategory.SUB_JUNIOR, 14, 15);
        assertRange(CompetitionAgeCategory.JUNIOR, 16, 18);
        assertRange(CompetitionAgeCategory.YOUTH, 19, 23);
    }

    @Test
    void acceptsUnlimitedSeniorAndMastersMinimums() {
        assertNull(CompetitionAgeCategory.SENIOR.getMinimumAge());
        assertNull(CompetitionAgeCategory.SENIOR.getMaximumAge());
        assertTrue(CompetitionAgeCategory.SENIOR.acceptsAge(0));
        assertTrue(CompetitionAgeCategory.SENIOR.acceptsAge(120));
        assertNull(CompetitionAgeCategory.AMATEUR.getMinimumAge());
        assertNull(CompetitionAgeCategory.AMATEUR.getMaximumAge());
        assertTrue(CompetitionAgeCategory.AMATEUR.acceptsAge(0));
        assertTrue(CompetitionAgeCategory.AMATEUR.acceptsAge(120));
        assertEquals(Shift.MORNING, CompetitionAgeCategory.AMATEUR.getShift());

        assertMinimumOnly(CompetitionAgeCategory.MASTER, 40);
        assertMinimumOnly(CompetitionAgeCategory.GRAND_MASTER, 50);
        assertMinimumOnly(CompetitionAgeCategory.SENIOR_GRAND_MASTER, 60);
        assertMinimumOnly(CompetitionAgeCategory.SUPER_SENIOR_GRAND_MASTER, 70);
    }

    @Test
    void calculatesAgeByCompetitionCalendarYear() {
        LocalDate bornOnNewYearsEve = LocalDate.of(2010, 12, 31);
        assertTrue(CompetitionAgeCategory.SUB_JUNIOR.acceptsBirthDateForCompetitionYear(bornOnNewYearsEve, 2024));
        assertFalse(CompetitionAgeCategory.JUNIOR.acceptsBirthDateForCompetitionYear(bornOnNewYearsEve, 2024));
    }

    private static void assertRange(CompetitionAgeCategory category, int minimum, int maximum) {
        assertTrue(category.acceptsAge(minimum));
        assertTrue(category.acceptsAge(maximum));
        assertFalse(category.acceptsAge(minimum - 1));
        assertFalse(category.acceptsAge(maximum + 1));
    }

    private static void assertMinimumOnly(CompetitionAgeCategory category, int minimum) {
        assertEquals(minimum, category.getMinimumAge());
        assertNull(category.getMaximumAge());
        assertFalse(category.acceptsAge(minimum - 1));
        assertTrue(category.acceptsAge(minimum));
        assertTrue(category.acceptsAge(120));
    }
}
