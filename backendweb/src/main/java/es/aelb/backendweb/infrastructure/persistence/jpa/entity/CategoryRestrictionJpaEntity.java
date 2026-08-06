package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "category_restrictions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pricing_config_id", "category_type_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class CategoryRestrictionJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_config_id", nullable = false)
    private PricingConfigJpaEntity pricingConfig;

    @Column(name = "category_type_id", length = 100, nullable = false)
    private String categoryTypeId;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "max_experience_years")
    private Integer maxExperienceYears;

    @Column(name = "max_gold_medals")
    private Integer maxGoldMedals;

    /** IDs de tipos de categoría incompatibles, separados por coma. */
    @Column(name = "incompatible_with", columnDefinition = "TEXT")
    private String incompatibleWith;

    /** IDs de tipos de categoría requeridos para combinación, separados por coma. */
    @Column(name = "requires_combination", columnDefinition = "TEXT")
    private String requiresCombination;
}
