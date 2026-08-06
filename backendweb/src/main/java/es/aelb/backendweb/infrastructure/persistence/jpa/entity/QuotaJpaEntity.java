package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotas")
@Getter @Setter @NoArgsConstructor
public class QuotaJpaEntity {
    @Id @Column(length = 36) private String id;
    @Column(name = "user_id", nullable = false, length = 36) private String userId;
    @Column(nullable = false) private int year;
    @Enumerated(EnumType.STRING) @Column(name = "age_category", nullable = false, length = 30) private AgeCategoryJpa ageCategory;
    @Column(nullable = false, precision = 8, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 10) private PaymentStatusJpa paymentStatus;
    @Column(name = "payment_date") private LocalDate paymentDate;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Enumerated(EnumType.STRING) @Column(name = "payment_source", length = 10) private PaymentSourceJpa paymentSource;
    @Column(name = "paid_by_user_id", length = 36) private String paidByUserId;
    @Column(name = "payment_reference", length = 255) private String paymentReference;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public enum AgeCategoryJpa { SUB_JUNIOR, JUNIOR, YOUTH, SENIOR, MASTERS, GRAND_MASTERS, SENIOR_GRAND_MASTERS }
    public enum PaymentStatusJpa { PENDING, PAID }
    public enum PaymentSourceJpa { STRIPE, MANUAL, LEGACY }
}
