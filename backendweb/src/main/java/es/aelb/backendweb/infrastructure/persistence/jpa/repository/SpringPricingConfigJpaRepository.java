package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.PricingConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringPricingConfigJpaRepository extends JpaRepository<PricingConfigJpaEntity, String> {
    Optional<PricingConfigJpaEntity> findByChampionshipId(String championshipId);
}
