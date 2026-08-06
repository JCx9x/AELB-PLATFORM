package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "championship_age_category_prices", uniqueConstraints = @UniqueConstraint(columnNames = {"championship_id", "age_category"}))
@Getter @Setter @NoArgsConstructor
public class ChampionshipAgeCategoryPriceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "championship_id", nullable = false, length = 36) private String championshipId;
    @Enumerated(EnumType.STRING) @Column(name = "age_category", nullable = false, length = 40) private CompetitionAgeCategory ageCategory;
    @Column(name = "price_per_arm", nullable = false, precision = 8, scale = 2) private BigDecimal pricePerArm;
    @Column(name = "combination_price", nullable = false, precision = 8, scale = 2) private BigDecimal combinationPrice;
}
