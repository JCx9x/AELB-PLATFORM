package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import es.aelb.backendweb.domain.pricing.checkout.CheckoutStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration_checkouts")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationCheckoutJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "championship_id", length = 36, nullable = false)
    private String championshipId;

    /** JSON array de líneas: [{"concept":"...","amount":"20.00","ruleId":"..."}] */
    @Column(name = "line_items_json", columnDefinition = "TEXT", nullable = false)
    private String lineItemsJson;

    @Column(name = "total", nullable = false, precision = 8, scale = 2)
    private BigDecimal total;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /** JSON object de metadatos: {"userId":"...","categoryTypes":"SENIOR,MASTER",...} */
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "basket_key", length = 64)
    private String basketKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CheckoutStatus status;

    @Column(name = "stripe_session_id", length = 255, unique = true)
    private String stripeSessionId;

    @Column(name = "payment_reference", length = 200)
    private String paymentReference;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
