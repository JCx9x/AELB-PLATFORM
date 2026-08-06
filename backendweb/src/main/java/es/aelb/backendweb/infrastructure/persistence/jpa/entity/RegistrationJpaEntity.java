package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "registrations")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "championship_id", nullable = false, length = 36)
    private String championshipId;

    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;

    @Column(name = "amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private PaymentStatusJpa paymentStatus;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "registered_by", nullable = false, length = 36)
    private String registeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum PaymentStatusJpa { PENDING, PAID, CANCELLED }
}
