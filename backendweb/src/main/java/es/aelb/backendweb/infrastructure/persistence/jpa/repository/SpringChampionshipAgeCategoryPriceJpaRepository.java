package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.ChampionshipAgeCategoryPriceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringChampionshipAgeCategoryPriceJpaRepository extends JpaRepository<ChampionshipAgeCategoryPriceJpaEntity, Long> {
    List<ChampionshipAgeCategoryPriceJpaEntity> findByChampionshipId(String championshipId);
    void deleteByChampionshipId(String championshipId);
}
