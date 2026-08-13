package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.ResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringResultJpaRepository extends JpaRepository<ResultJpaEntity, String> {

    List<ResultJpaEntity> findByPublishedTrueOrderByPublishedAtDesc();
    Optional<ResultJpaEntity> findByIdAndPublishedTrue(String id);
    List<ResultJpaEntity> findAllByOrderByCreatedAtDesc();
}
