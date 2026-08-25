package es.aelb.backendweb.infrastructure.observability;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.EmailOutboxJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringEmailOutboxJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Exposes a small backlog gauge; status is a bounded dimension. */
@Component
public final class EmailOutboxMetrics {
    public EmailOutboxMetrics(MeterRegistry registry, SpringEmailOutboxJpaRepository repository) {
        Gauge.builder("aelb.email.outbox.pending", repository,
                        r -> r.countByStatus(EmailOutboxJpaEntity.Status.PENDING))
                .description("Pending transactional email jobs")
                .register(registry);
        Gauge.builder("aelb.email.outbox.failed", repository,
                        r -> r.countByStatus(EmailOutboxJpaEntity.Status.FAILED))
                .description("Failed transactional email jobs")
                .register(registry);
    }
}
