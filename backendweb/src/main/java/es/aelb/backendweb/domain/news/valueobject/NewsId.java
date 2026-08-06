package es.aelb.backendweb.domain.news.valueobject;

import java.util.UUID;

public record NewsId(String value) {

    public NewsId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NewsId no puede estar vacío");
        }
    }

    public static NewsId generate() { return new NewsId(UUID.randomUUID().toString()); }
    public static NewsId of(String value) { return new NewsId(value); }

    @Override
    public String toString() { return value; }
}
