package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.NewsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SpringNewsJpaRepository extends JpaRepository<NewsJpaEntity, String> {

    List<NewsJpaEntity> findByPublishedTrueOrderByPublishedAtDesc();
    Optional<NewsJpaEntity> findByIdAndPublishedTrue(String id);
    List<NewsJpaEntity> findAllByOrderByCreatedAtDesc();
    Page<NewsJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<NewsJpaEntity> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
}
