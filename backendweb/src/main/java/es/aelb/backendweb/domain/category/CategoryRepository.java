package es.aelb.backendweb.domain.category;

import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.domain.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    void               save(Category category);
    Optional<Category> findById(CategoryId id);
    boolean            existsByName(String name);
    boolean            existsByNameAndIdNot(String name, CategoryId excludeId);
    List<Category>     findAll();
    PageResult<Category> findPage(int page, int size);
    void               delete(CategoryId id);
}
