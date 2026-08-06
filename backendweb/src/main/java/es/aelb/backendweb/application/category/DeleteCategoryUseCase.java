package es.aelb.backendweb.application.category;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;

public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public DeleteCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void execute(String categoryId) {
        CategoryId id = CategoryId.of(categoryId);
        if (categoryRepository.findById(id).isEmpty()) {
            throw new Category.NotFoundException(categoryId);
        }
        categoryRepository.delete(id);
    }
}
