package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import es.aelb.backendweb.application.email.EmailType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_outbox", uniqueConstraints = @UniqueConstraint(name = "uq_email_outbox_idempotency", columnNames = "idempotency_key"))
@Getter @Setter @NoArgsConstructor
public class EmailOutboxJpaEntity {
    @Id @Column(length = 36, nullable = false) private String id = UUID.randomUUID().toString();
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private EmailType type;
    @Column(nullable = false, length = 255) private String recipient;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private Status status = Status.PENDING;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private LocalDateTime nextAttemptAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "idempotency_key", nullable = false, length = 255) private String idempotencyKey;
    @Column(name = "processing_started_at") private LocalDateTime processingStartedAt;
    public enum Status { PENDING, PROCESSING, SENT, FAILED }
    @PrePersist void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); if (nextAttemptAt == null) nextAttemptAt = createdAt; }
}
