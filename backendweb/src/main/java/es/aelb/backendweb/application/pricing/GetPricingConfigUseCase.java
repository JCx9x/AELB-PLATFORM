package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.PricingConfig;
import es.aelb.backendweb.domain.pricing.PricingConfigRepository;
import es.aelb.backendweb.domain.pricing.PricingException;

public class GetPricingConfigUseCase implements UseCase<String, PricingConfig> {

    private final PricingConfigRepository repository;

    public GetPricingConfigUseCase(PricingConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public PricingConfig execute(String championshipId) {
        return repository.findByChampionshipId(ChampionshipId.of(championshipId))
                .orElseThrow(() -> new PricingException.ConfigNotFoundException(championshipId));
    }
}
