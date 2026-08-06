package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.ChampionshipJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringChampionshipJpaRepository extends JpaRepository<ChampionshipJpaEntity, String> {

    List<ChampionshipJpaEntity> findByVisibleTrue();
}
