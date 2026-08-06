package es.aelb.backendweb.domain.category.valueobject;

import java.util.UUID;

public record CategoryId(String value) {

    public CategoryId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CategoryId no puede estar vacío");
        }
    }

    public static CategoryId generate() {
        return new CategoryId(UUID.randomUUID().toString());
    }

    public static CategoryId of(String value) {
        return new CategoryId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
