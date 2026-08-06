package es.aelb.backendweb.application.category;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;

public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CreateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category execute(CreateCategoryCommand cmd) {
        Category category = Category.create(
                cmd.gender(), cmd.armSide(), cmd.weightLimit(), cmd.ageGroup(), cmd.ageCategory(), cmd.shift()
        );
        if (categoryRepository.existsByName(category.getName())) {
            throw new Category.NameAlreadyExistsException(category.getName());
        }
        categoryRepository.save(category);
        return category;
    }
}
