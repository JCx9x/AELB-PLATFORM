package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.domain.category.ArmSide;

import java.util.List;
import java.util.Map;

/**
 * Prepara el checkout a partir de un cálculo de precio ya validado.
 * El motor recalcula el precio internamente (el cliente no pasa el importe).
 */
public record PrepareCheckoutCommand(
        String                 userId,
        String                 championshipId,
        List<EntryData>        entries,
        Integer                athleteAge,
        Integer                experienceYears,
        Integer                goldMedals,
        Map<String, String>    extraMetadata
) {
    public record EntryData(String categoryTypeId, ArmSide armSide) {}
}
