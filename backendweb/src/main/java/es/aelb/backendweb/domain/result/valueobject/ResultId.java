package es.aelb.backendweb.domain.result.valueobject;

import java.util.UUID;

public record ResultId(String value) {

    public ResultId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ResultId no puede estar vacío");
        }
    }

    public static ResultId generate() { return new ResultId(UUID.randomUUID().toString()); }
    public static ResultId of(String value) { return new ResultId(value); }

    @Override
    public String toString() { return value; }
}
