package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.CategoryJpaEntity;

public class CategoryJpaMapper {

    public static CategoryJpaEntity toJpa(Category category) {
        return new CategoryJpaEntity(
                category.getId().value(),
                category.getName(),
                category.getGender(),
                category.getArmSide(),
                category.getWeightLimit(),
                category.getAgeGroup(),
                category.getAgeCategory(),
                category.getShift()
        );
    }

    public static Category toDomain(CategoryJpaEntity entity) {
        return Category.reconstitute(
                CategoryId.of(entity.getId()),
                entity.getName(),
                entity.getGender(),
                entity.getArmSide(),
                entity.getWeightLimit(),
                entity.getAgeGroup(),
                entity.getAgeCategory(),
                entity.getShift()
        );
    }

    private CategoryJpaMapper() {}
}
