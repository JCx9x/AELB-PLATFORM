package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "annual_quota_prices")
@Getter @Setter @NoArgsConstructor
public class AnnualQuotaPriceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private int year;
    @Enumerated(EnumType.STRING) @Column(name = "age_category", nullable = false, length = 30) private QuotaAgeCategoryJpa ageCategory;
    @Column(nullable = false, precision = 8, scale = 2) private BigDecimal amount;
    public enum QuotaAgeCategoryJpa { SUB_JUNIOR, JUNIOR, YOUTH, SENIOR, MASTERS, GRAND_MASTERS, SENIOR_GRAND_MASTERS }
}
