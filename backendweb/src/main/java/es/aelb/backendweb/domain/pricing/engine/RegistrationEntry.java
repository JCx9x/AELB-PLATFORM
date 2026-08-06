package es.aelb.backendweb.domain.pricing.engine;

import es.aelb.backendweb.domain.category.ArmSide;

/**
 * Representa una entrada en el basket de inscripción.
 *
 * categoryTypeId : tipo de categoría (p.ej. "SENIOR", "JUNIOR_18", "MASTER")
 *                  corresponde al campo `age_group` de la entidad Category
 * armSide        : brazo para el que se inscribe
 */
public record RegistrationEntry(String categoryTypeId, ArmSide armSide) {

    public RegistrationEntry {
        if (categoryTypeId == null || categoryTypeId.isBlank())
            throw new IllegalArgumentException("categoryTypeId no puede estar vacío");
        if (armSide == null)
            throw new IllegalArgumentException("armSide no puede ser nulo");
    }

    public static RegistrationEntry of(String categoryTypeId, ArmSide armSide) {
        return new RegistrationEntry(categoryTypeId, armSide);
    }
}
