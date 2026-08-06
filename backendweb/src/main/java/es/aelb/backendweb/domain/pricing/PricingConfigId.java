package es.aelb.backendweb.domain.pricing;

import java.util.UUID;

public record PricingConfigId(String value) {

    public PricingConfigId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("PricingConfigId no puede estar vacío");
    }

    public static PricingConfigId generate() { return new PricingConfigId(UUID.randomUUID().toString()); }
    public static PricingConfigId of(String value) { return new PricingConfigId(value); }

    @Override
    public String toString() { return value; }
}
