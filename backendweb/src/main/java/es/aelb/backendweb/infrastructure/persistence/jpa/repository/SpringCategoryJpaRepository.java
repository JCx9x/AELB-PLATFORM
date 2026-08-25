package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpringCategoryJpaRepository extends JpaRepository<CategoryJpaEntity, String> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);
    Page<CategoryJpaEntity> findAll(Pageable pageable);
}
