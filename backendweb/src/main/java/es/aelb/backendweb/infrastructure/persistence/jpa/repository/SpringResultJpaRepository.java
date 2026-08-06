package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.ResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringResultJpaRepository extends JpaRepository<ResultJpaEntity, String> {

    List<ResultJpaEntity> findByPublishedTrueOrderByPublishedAtDesc();
    List<ResultJpaEntity> findAllByOrderByCreatedAtDesc();
}
