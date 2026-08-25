package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.DocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SpringDocumentJpaRepository extends JpaRepository<DocumentJpaEntity, String> {

    List<DocumentJpaEntity> findByPublishedTrueOrderByPublishedAtDesc();
    Optional<DocumentJpaEntity> findByIdAndPublishedTrue(String id);
    List<DocumentJpaEntity> findAllByOrderByCreatedAtDesc();
    Page<DocumentJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<DocumentJpaEntity> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
}
