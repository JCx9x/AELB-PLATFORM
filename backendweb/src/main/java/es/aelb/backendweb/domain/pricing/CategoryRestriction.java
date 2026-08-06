package es.aelb.backendweb.domain.pricing;

import java.util.Collections;
import java.util.Set;

/**
 * Restricciones de elegibilidad para un tipo de categoría concreto dentro de una configuración.
 *
 * Todas las restricciones son opcionales (null = sin restricción).
 * Si el atleta no cumple alguna condición, la inscripción es rechazada con mensaje descriptivo.
 */
public final class CategoryRestriction {

    private final String      categoryTypeId;
    private final Integer     ageMin;
    private final Integer     ageMax;
    private final Integer     maxExperienceYears;
    private final Integer     maxGoldMedals;
    private final Set<String> incompatibleWith;
    private final Set<String> requiresCombinationWith;

    public CategoryRestriction(
            String categoryTypeId,
            Integer ageMin,
            Integer ageMax,
            Integer maxExperienceYears,
            Integer maxGoldMedals,
            Set<String> incompatibleWith,
            Set<String> requiresCombinationWith
    ) {
        if (categoryTypeId == null || categoryTypeId.isBlank())
            throw new IllegalArgumentException("categoryTypeId no puede estar vacío");
        this.categoryTypeId          = categoryTypeId;
        this.ageMin                  = ageMin;
        this.ageMax                  = ageMax;
        this.maxExperienceYears      = maxExperienceYears;
        this.maxGoldMedals           = maxGoldMedals;
        this.incompatibleWith        = incompatibleWith != null ? Set.copyOf(incompatibleWith) : Collections.emptySet();
        this.requiresCombinationWith = requiresCombinationWith != null ? Set.copyOf(requiresCombinationWith) : Collections.emptySet();
    }

    public String      getCategoryTypeId()          { return categoryTypeId; }
    public Integer     getAgeMin()                   { return ageMin; }
    public Integer     getAgeMax()                   { return ageMax; }
    public Integer     getMaxExperienceYears()       { return maxExperienceYears; }
    public Integer     getMaxGoldMedals()            { return maxGoldMedals; }
    public Set<String> getIncompatibleWith()         { return incompatibleWith; }
    public Set<String> getRequiresCombinationWith()  { return requiresCombinationWith; }
}
