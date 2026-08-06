package es.aelb.backendweb.domain.pricing.engine;

/**
 * Contexto del atleta necesario para evaluar restricciones y reglas de precio.
 *
 * age               : edad en años completos
 * experienceYears   : años completos compitiendo (0 = primer año)
 * goldMedals        : medallas de oro acumuladas en la categoría
 */
public record AthleteContext(int age, int experienceYears, int goldMedals) {

    /** Contexto sin restricciones (para casos donde no se aplican reglas de elegibilidad). */
    public static AthleteContext unrestricted() {
        return new AthleteContext(Integer.MAX_VALUE, 0, 0);
    }

    public static AthleteContext of(int age, int experienceYears, int goldMedals) {
        return new AthleteContext(age, experienceYears, goldMedals);
    }
}
