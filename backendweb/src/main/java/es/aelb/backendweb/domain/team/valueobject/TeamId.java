package es.aelb.backendweb.domain.team.valueobject;

import java.util.UUID;

public record TeamId(String value) {

    public TeamId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TeamId no puede estar vacío");
        }
    }

    public static TeamId generate() {
        return new TeamId(UUID.randomUUID().toString());
    }

    public static TeamId of(String value) {
        return new TeamId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
