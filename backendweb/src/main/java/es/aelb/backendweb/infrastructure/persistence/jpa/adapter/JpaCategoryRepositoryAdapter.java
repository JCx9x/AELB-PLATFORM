package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.CategoryJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringCategoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final SpringCategoryJpaRepository jpaRepository;

    public JpaCategoryRepositoryAdapter(SpringCategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Category category) {
        jpaRepository.save(CategoryJpaMapper.toJpa(category));
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        return jpaRepository.findById(id.value())
                .map(CategoryJpaMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, CategoryId excludeId) {
        return jpaRepository.existsByNameAndIdNot(name, excludeId.value());
    }

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll().stream()
                .map(CategoryJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(CategoryId id) {
        jpaRepository.deleteById(id.value());
    }
}
