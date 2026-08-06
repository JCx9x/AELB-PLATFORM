package es.aelb.backendweb.domain.pricing;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Configuración de precios de un campeonato. Agregado raíz.
 *
 * Contiene:
 *  - Parámetros globales (límites, moneda)
 *  - Lista de reglas de precio (PricingRule)
 *  - Restricciones por tipo de categoría (CategoryRestriction)
 *
 * Una competición tiene exactamente una PricingConfig.
 * Reemplazar las reglas sustituye la lista completa (replace-all semántica).
 */
public class PricingConfig extends AggregateRoot<PricingConfigId> {

    private final ChampionshipId        championshipId;
    private       int                   maxCategoriesPerArm;
    private       int                   maxTotalEntries;
    private       String                currency;
    private       boolean               active;
    private final List<PricingRule>     rules;
    private final List<CategoryRestriction> categoryRestrictions;
    private final LocalDateTime         createdAt;
    private       LocalDateTime         updatedAt;

    private PricingConfig(
            PricingConfigId id,
            ChampionshipId championshipId,
            int maxCategoriesPerArm,
            int maxTotalEntries,
            String currency,
            boolean active,
            List<PricingRule> rules,
            List<CategoryRestriction> categoryRestrictions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        super(id);
        this.championshipId       = championshipId;
        this.maxCategoriesPerArm  = maxCategoriesPerArm;
        this.maxTotalEntries      = maxTotalEntries;
        this.currency             = currency;
        this.active               = active;
        this.rules                = new ArrayList<>(rules != null ? rules : Collections.emptyList());
        this.categoryRestrictions = new ArrayList<>(categoryRestrictions != null ? categoryRestrictions : Collections.emptyList());
        this.createdAt            = createdAt;
        this.updatedAt            = updatedAt;
    }

    public static PricingConfig create(
            ChampionshipId championshipId,
            int maxCategoriesPerArm,
            int maxTotalEntries,
            String currency,
            List<PricingRule> rules,
            List<CategoryRestriction> categoryRestrictions
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new PricingConfig(
                PricingConfigId.generate(),
                championshipId,
                maxCategoriesPerArm,
                maxTotalEntries,
                currency != null ? currency : "EUR",
                true,
                rules,
                categoryRestrictions,
                now, now
        );
    }

    public static PricingConfig reconstitute(
            PricingConfigId id,
            ChampionshipId championshipId,
            int maxCategoriesPerArm,
            int maxTotalEntries,
            String currency,
            boolean active,
            List<PricingRule> rules,
            List<CategoryRestriction> categoryRestrictions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new PricingConfig(id, championshipId, maxCategoriesPerArm, maxTotalEntries,
                currency, active, rules, categoryRestrictions, createdAt, updatedAt);
    }

    /**
     * Reemplaza todas las reglas. Se usa al actualizar la configuración desde un comando.
     */
    public void replaceRules(List<PricingRule> newRules) {
        this.rules.clear();
        if (newRules != null) this.rules.addAll(newRules);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reemplaza todas las restricciones de categoría.
     */
    public void replaceCategoryRestrictions(List<CategoryRestriction> restrictions) {
        this.categoryRestrictions.clear();
        if (restrictions != null) this.categoryRestrictions.addAll(restrictions);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateGlobalParams(int maxCategoriesPerArm, int maxTotalEntries, String currency) {
        this.maxCategoriesPerArm = maxCategoriesPerArm;
        this.maxTotalEntries     = maxTotalEntries;
        this.currency            = currency != null ? currency : "EUR";
        this.updatedAt           = LocalDateTime.now();
    }

    public void activate()   { this.active = true;  this.updatedAt = LocalDateTime.now(); }
    public void deactivate() { this.active = false; this.updatedAt = LocalDateTime.now(); }

    /** Reglas activas ordenadas por prioridad ascendente (menor número = mayor prioridad). */
    public List<PricingRule> getActiveRulesSorted() {
        return rules.stream()
                .filter(PricingRule::isActive)
                .sorted(Comparator.comparingInt(PricingRule::getPriority))
                .toList();
    }

    public List<PricingRule> getActiveRules() {
        return rules.stream().filter(PricingRule::isActive).toList();
    }

    public ChampionshipId           getChampionshipId()        { return championshipId; }
    public int                      getMaxCategoriesPerArm()   { return maxCategoriesPerArm; }
    public int                      getMaxTotalEntries()       { return maxTotalEntries; }
    public String                   getCurrency()              { return currency; }
    public boolean                  isActive()                 { return active; }
    public List<PricingRule>        getRules()                 { return Collections.unmodifiableList(rules); }
    public List<CategoryRestriction> getCategoryRestrictions() { return Collections.unmodifiableList(categoryRestrictions); }
    public LocalDateTime            getCreatedAt()             { return createdAt; }
    public LocalDateTime            getUpdatedAt()             { return updatedAt; }
}
