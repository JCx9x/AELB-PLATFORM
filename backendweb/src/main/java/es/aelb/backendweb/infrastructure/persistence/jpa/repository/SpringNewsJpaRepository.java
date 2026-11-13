package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.NewsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringNewsJpaRepository extends JpaRepository<NewsJpaEntity, String> {

    List<NewsJpaEntity> findByPublishedTrueOrderByPublishedAtDesc();
    Optional<NewsJpaEntity> findByIdAndPublishedTrue(String id);
    List<NewsJpaEntity> findAllByOrderByCreatedAtDesc();
    Page<NewsJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<NewsJpaEntity> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    @Query("""
            SELECT n FROM NewsJpaEntity n
            WHERE n.published = true
              AND (LOWER(n.title) LIKE :pattern ESCAPE '!' OR LOWER(CAST(n.content AS String)) LIKE :pattern ESCAPE '!')
            ORDER BY n.publishedAt DESC
            """)
    Page<NewsJpaEntity> searchPublished(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
            SELECT n FROM NewsJpaEntity n
            WHERE LOWER(n.title) LIKE :pattern ESCAPE '!' OR LOWER(CAST(n.content AS String)) LIKE :pattern ESCAPE '!'
            ORDER BY n.createdAt DESC
            """)
    Page<NewsJpaEntity> searchAll(@Param("pattern") String pattern, Pageable pageable);
}
