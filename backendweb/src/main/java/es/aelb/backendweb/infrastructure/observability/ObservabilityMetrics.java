package es.aelb.backendweb.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Small, bounded-cardinality business metrics facade. IDs belong in logs, never in tags. */
@Component
public final class ObservabilityMetrics {
    private final MeterRegistry registry;
    public ObservabilityMetrics(MeterRegistry registry) { this.registry = registry; }

    public void stripeWebhook(String result) { counter("aelb.stripe.webhooks", "result", result).increment(); }
    public void payment(String result) { counter("aelb.payments", "result", result).increment(); }
    public void registration(String result) { counter("aelb.registrations", "result", result).increment(); }
    public void email(String result) { counter("aelb.emails", "result", result).increment(); }

    private Counter counter(String name, String tag, String value) {
        return Counter.builder(name).description("AELB business operation count").tag(tag, value).register(registry);
    }
}
