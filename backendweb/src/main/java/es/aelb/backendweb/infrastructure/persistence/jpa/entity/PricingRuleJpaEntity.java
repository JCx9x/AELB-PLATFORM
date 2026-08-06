package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
public class PricingRuleJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_config_id", nullable = false)
    private PricingConfigJpaEntity pricingConfig;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "active", nullable = false)
    private boolean active;

    /** JSON array de condiciones: [{"type":"AGE_MAX","value":"15"}, ...] */
    @Column(name = "conditions_json", columnDefinition = "TEXT", nullable = false)
    private String conditionsJson;

    /** JSON objeto del efecto: {"type":"BUNDLE_FIXED_TOTAL","amount":"20.00","description":"..."} */
    @Column(name = "effect_json", columnDefinition = "TEXT", nullable = false)
    private String effectJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
