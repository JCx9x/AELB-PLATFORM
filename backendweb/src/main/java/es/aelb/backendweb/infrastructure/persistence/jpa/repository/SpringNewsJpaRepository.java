package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.NewsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringNewsJpaRepository extends JpaRepository<NewsJpaEntity, String> {

    List<NewsJpaEntity> findByPublishedTrueOrderByPublishedAtDesc();
    List<NewsJpaEntity> findAllByOrderByCreatedAtDesc();
}
