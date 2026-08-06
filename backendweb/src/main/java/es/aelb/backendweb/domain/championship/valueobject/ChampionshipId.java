package es.aelb.backendweb.domain.championship.valueobject;

import java.util.UUID;

public record ChampionshipId(String value) {

    public ChampionshipId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ChampionshipId no puede estar vacío");
        }
    }

    public static ChampionshipId generate() {
        return new ChampionshipId(UUID.randomUUID().toString());
    }

    public static ChampionshipId of(String value) {
        return new ChampionshipId(value);
    }

    @Override
    public String toString() { return value; }
}
