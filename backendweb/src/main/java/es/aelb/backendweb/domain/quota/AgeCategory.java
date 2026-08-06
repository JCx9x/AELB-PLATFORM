package es.aelb.backendweb.domain.quota;

/** Tramos de cuota anual. La edad se evalúa al 31 de diciembre del año de cuota. */
public enum AgeCategory {
    SUB_JUNIOR("Sub Junior (U15)", 14, 15),
    JUNIOR("Junior (U18)", 16, 18),
    YOUTH("Youth (U23)", 19, 23),
    SENIOR("Senior", null, null),
    MASTERS("Masters", 40, 49),
    GRAND_MASTERS("Grand Masters", 50, 59),
    SENIOR_GRAND_MASTERS("Senior Grand Masters", 60, null);

    private final String label;
    private final Integer minimumAge;
    private final Integer maximumAge;

    AgeCategory(String label, Integer minimumAge, Integer maximumAge) {
        this.label = label;
        this.minimumAge = minimumAge;
        this.maximumAge = maximumAge;
    }

    public String getLabel() { return label; }
    public Integer getMinimumAge() { return minimumAge; }
    public Integer getMaximumAge() { return maximumAge; }

    public static AgeCategory fromAge(int age) {
        if (age >= 60) return SENIOR_GRAND_MASTERS;
        if (age >= 50) return GRAND_MASTERS;
        if (age >= 40) return MASTERS;
        if (age >= 19 && age <= 23) return YOUTH;
        if (age >= 16 && age <= 18) return JUNIOR;
        if (age >= 14 && age <= 15) return SUB_JUNIOR;
        return SENIOR;
    }
}
