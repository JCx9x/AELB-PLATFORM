package es.aelb.backendweb.domain.category;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Shift {
    MORNING,
    AFTERNOON;

    /** Acepta los valores que exponía la API antes de normalizar los turnos. */
    @JsonCreator
    public static Shift fromValue(String value) {
        return switch (value) {
            case "TURNO_1" -> MORNING;
            case "TURNO_2" -> AFTERNOON;
            default -> Shift.valueOf(value);
        };
    }
}
