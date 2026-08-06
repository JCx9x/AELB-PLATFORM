package es.aelb.backendweb.domain.pricing;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;

import java.util.Optional;

public interface PricingConfigRepository {
    void save(PricingConfig config);
    Optional<PricingConfig> findByChampionshipId(ChampionshipId championshipId);
    Optional<PricingConfig> findById(PricingConfigId id);
    void delete(PricingConfigId id);
}
