package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.*;

import java.util.List;
import java.util.Optional;

/**
 * Crea o reemplaza la configuración de precios de un campeonato.
 * Operación idempotente: si ya existe, actualiza; si no, crea.
 */
public class SavePricingConfigUseCase implements UseCase<SavePricingConfigCommand, String> {

    private final PricingConfigRepository repository;

    public SavePricingConfigUseCase(PricingConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public String execute(SavePricingConfigCommand cmd) {
        ChampionshipId championshipId = ChampionshipId.of(cmd.championshipId());

        List<PricingRule> rules = cmd.rules().stream()
                .map(r -> PricingRule.create(
                        r.name(),
                        r.description(),
                        r.priority(),
                        r.active(),
                        r.conditions().stream()
                                .map(c -> RuleCondition.of(c.type(), c.value()))
                                .toList(),
                        PricingEffect.of(r.effect().type(), r.effect().amount(), r.effect().description())
                ))
                .toList();

        List<CategoryRestriction> restrictions = cmd.categoryRestrictions().stream()
                .map(r -> new CategoryRestriction(
                        r.categoryTypeId(),
                        r.ageMin(),
                        r.ageMax(),
                        r.maxExperienceYears(),
                        r.maxGoldMedals(),
                        r.incompatibleWith(),
                        r.requiresCombinationWith()
                ))
                .toList();

        Optional<PricingConfig> existing = repository.findByChampionshipId(championshipId);

        PricingConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.updateGlobalParams(cmd.maxCategoriesPerArm(), cmd.maxTotalEntries(), cmd.currency());
            config.replaceRules(rules);
            config.replaceCategoryRestrictions(restrictions);
        } else {
            config = PricingConfig.create(
                    championshipId,
                    cmd.maxCategoriesPerArm(),
                    cmd.maxTotalEntries(),
                    cmd.currency(),
                    rules,
                    restrictions
            );
        }

        repository.save(config);
        return config.getId().value();
    }
}
