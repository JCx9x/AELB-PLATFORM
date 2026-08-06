package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.PricingConfig;
import es.aelb.backendweb.domain.pricing.PricingConfigId;
import es.aelb.backendweb.domain.pricing.PricingConfigRepository;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.PricingConfigJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringPricingConfigJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaPricingConfigRepositoryAdapter implements PricingConfigRepository {

    private final SpringPricingConfigJpaRepository jpaRepository;

    public JpaPricingConfigRepositoryAdapter(SpringPricingConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(PricingConfig config) {
        jpaRepository.save(PricingConfigJpaMapper.toJpa(config));
    }

    @Override
    public Optional<PricingConfig> findByChampionshipId(ChampionshipId championshipId) {
        return jpaRepository.findByChampionshipId(championshipId.value())
                .map(PricingConfigJpaMapper::toDomain);
    }

    @Override
    public Optional<PricingConfig> findById(PricingConfigId id) {
        return jpaRepository.findById(id.value())
                .map(PricingConfigJpaMapper::toDomain);
    }

    @Override
    public void delete(PricingConfigId id) {
        jpaRepository.deleteById(id.value());
    }
}
