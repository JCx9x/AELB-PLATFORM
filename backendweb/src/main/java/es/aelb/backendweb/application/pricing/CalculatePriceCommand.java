package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.domain.category.ArmSide;

import java.util.List;

/**
 * Comando para calcular el precio de un basket de inscripción.
 *
 * entries        : lista de (categoryTypeId, armSide) que el atleta quiere inscribir
 * athleteAge     : edad en años completos (null = sin restricción de edad)
 * experienceYears: años completos compitiendo (null = 0)
 * goldMedals     : medallas de oro acumuladas (null = 0)
 */
public record CalculatePriceCommand(
        String championshipId,
        List<EntryData> entries,
        Integer athleteAge,
        Integer experienceYears,
        Integer goldMedals
) {
    public record EntryData(String categoryTypeId, ArmSide armSide) {}
}
