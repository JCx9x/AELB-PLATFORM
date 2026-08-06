package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.DocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDocumentJpaRepository extends JpaRepository<DocumentJpaEntity, String> {

    List<DocumentJpaEntity> findByPublishedTrueOrderByPublishedAtDesc();
    List<DocumentJpaEntity> findAllByOrderByCreatedAtDesc();
}
