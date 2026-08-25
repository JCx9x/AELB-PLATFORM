package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.TeamJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpringTeamJpaRepository extends JpaRepository<TeamJpaEntity, String> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);
    Page<TeamJpaEntity> findAll(Pageable pageable);
}
