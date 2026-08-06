package es.aelb.backendweb.domain.pricing;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Regla de precio configurable.
 *
 * Cada regla tiene:
 *  - Una lista de condiciones (AND lógico): si todas se cumplen, la regla se activa.
 *  - Un efecto: qué hace al precio cuando se activa.
 *  - Una prioridad: número menor = se evalúa antes (importante para bundles).
 *
 * Las reglas son entidades dentro del agregado PricingConfig.
 */
public final class PricingRule {

    private final String            id;
    private       String            name;
    private       String            description;
    private       int               priority;
    private       boolean           active;
    private       List<RuleCondition> conditions;
    private       PricingEffect     effect;

    private PricingRule(
            String id,
            String name,
            String description,
            int priority,
            boolean active,
            List<RuleCondition> conditions,
            PricingEffect effect
    ) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.priority    = priority;
        this.active      = active;
        this.conditions  = conditions != null ? List.copyOf(conditions) : Collections.emptyList();
        this.effect      = effect;
    }

    public static PricingRule create(
            String name,
            String description,
            int priority,
            boolean active,
            List<RuleCondition> conditions,
            PricingEffect effect
    ) {
        validate(name, effect);
        return new PricingRule(UUID.randomUUID().toString(), name, description, priority, active, conditions, effect);
    }

    public static PricingRule reconstitute(
            String id,
            String name,
            String description,
            int priority,
            boolean active,
            List<RuleCondition> conditions,
            PricingEffect effect
    ) {
        return new PricingRule(id, name, description, priority, active, conditions, effect);
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }

    private static void validate(String name, PricingEffect effect) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre de la regla no puede estar vacío");
        if (effect == null) throw new IllegalArgumentException("El efecto de la regla no puede ser nulo");
    }

    public String              getId()          { return id; }
    public String              getName()        { return name; }
    public String              getDescription() { return description; }
    public int                 getPriority()    { return priority; }
    public boolean             isActive()       { return active; }
    public List<RuleCondition> getConditions()  { return conditions; }
    public PricingEffect       getEffect()      { return effect; }
}
