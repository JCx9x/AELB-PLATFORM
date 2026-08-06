package es.aelb.backendweb.domain.category;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Categorías oficiales WAF por edad. El turno forma parte de la categoría y
 * nunca se infiere a partir de sus límites de edad.
 */
public enum CompetitionAgeCategory {
    SUB_JUNIOR("Sub Junior", "U15", 14, 15, Shift.MORNING),
    JUNIOR("Junior", "U18", 16, 18, Shift.MORNING),
    YOUTH("Youth", "U23", 19, 23, Shift.MORNING),
    AMATEUR("Amateur", null, null, null, Shift.MORNING),
    SENIOR("Senior", null, null, null, Shift.AFTERNOON),
    MASTER("Master", null, 40, null, Shift.MORNING),
    GRAND_MASTER("Grand Master", null, 50, null, Shift.MORNING),
    SENIOR_GRAND_MASTER("Senior Grand Master", null, 60, null, Shift.MORNING),
    SUPER_SENIOR_GRAND_MASTER("Super Senior Grand Master", null, 70, null, Shift.MORNING);

    private final String displayName;
    private final String denomination;
    private final Integer minimumAge;
    private final Integer maximumAge;
    private final Shift shift;

    CompetitionAgeCategory(String displayName, String denomination, Integer minimumAge, Integer maximumAge, Shift shift) {
        this.displayName = displayName;
        this.denomination = denomination;
        this.minimumAge = minimumAge;
        this.maximumAge = maximumAge;
        this.shift = shift;
    }

    public String getDisplayName() { return displayName; }
    public String getDenomination() { return denomination; }
    public Integer getMinimumAge() { return minimumAge; }
    public Integer getMaximumAge() { return maximumAge; }
    public Shift getShift() { return shift; }

    public boolean acceptsAge(int age) {
        return (minimumAge == null || age >= minimumAge)
                && (maximumAge == null || age <= maximumAge);
    }

    /** La edad WAF se calcula por año natural, no por el día del evento. */
    public boolean acceptsBirthDateForCompetitionYear(LocalDate birthDate, int competitionYear) {
        if (birthDate == null) return false;
        return acceptsAge(competitionYear - birthDate.getYear());
    }

    public static Optional<CompetitionAgeCategory> fromIdentifier(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MASTERS" -> Optional.of(MASTER);
            case "GRAND_MASTERS" -> Optional.of(GRAND_MASTER);
            case "SENIOR_GRAND_MASTERS" -> Optional.of(SENIOR_GRAND_MASTER);
            default -> {
                try {
                    yield Optional.of(valueOf(normalized));
                } catch (IllegalArgumentException ex) {
                    yield Optional.empty();
                }
            }
        };
    }
}
