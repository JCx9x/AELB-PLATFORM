package es.aelb.backendweb.domain.category;

import es.aelb.backendweb.domain.category.valueobject.CategoryId;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    void               save(Category category);
    Optional<Category> findById(CategoryId id);
    boolean            existsByName(String name);
    boolean            existsByNameAndIdNot(String name, CategoryId excludeId);
    List<Category>     findAll();
    void               delete(CategoryId id);
}
