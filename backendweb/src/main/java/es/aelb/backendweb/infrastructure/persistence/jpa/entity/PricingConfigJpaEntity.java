package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pricing_configs")
@Getter
@Setter
@NoArgsConstructor
public class PricingConfigJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "championship_id", length = 36, nullable = false, unique = true)
    private String championshipId;

    @Column(name = "max_categories_per_arm", nullable = false)
    private int maxCategoriesPerArm;

    @Column(name = "max_total_entries", nullable = false)
    private int maxTotalEntries;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy      = "pricingConfig",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.EAGER
    )
    @OrderBy("priority ASC")
    private List<PricingRuleJpaEntity> rules = new ArrayList<>();

    @OneToMany(
            mappedBy      = "pricingConfig",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.EAGER
    )
    private List<CategoryRestrictionJpaEntity> categoryRestrictions = new ArrayList<>();
}
