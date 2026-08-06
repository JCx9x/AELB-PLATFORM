package es.aelb.backendweb.domain.document.valueobject;

import java.util.UUID;

public record DocumentId(String value) {

    public DocumentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DocumentId no puede estar vacío");
        }
    }

    public static DocumentId generate() { return new DocumentId(UUID.randomUUID().toString()); }
    public static DocumentId of(String value) { return new DocumentId(value); }

    @Override
    public String toString() { return value; }
}
