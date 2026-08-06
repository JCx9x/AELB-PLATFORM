package es.aelb.backendweb.application.category;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;

public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category execute(UpdateCategoryCommand cmd) {
        CategoryId id = CategoryId.of(cmd.categoryId());

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new Category.NotFoundException(cmd.categoryId()));

        String generatedName = Category.titleFor(cmd.gender(), cmd.armSide(), cmd.weightLimit(), cmd.ageGroup(), cmd.ageCategory());
        if (!category.getName().equals(generatedName)
                && categoryRepository.existsByNameAndIdNot(generatedName, id)) {
            throw new Category.NameAlreadyExistsException(generatedName);
        }

        category.update(cmd.gender(), cmd.armSide(), cmd.weightLimit(), cmd.ageGroup(), cmd.ageCategory(), cmd.shift());
        categoryRepository.save(category);
        return category;
    }
}
