package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.category.Category;

import java.math.BigDecimal;

public record CategoryResponse(
        String     id,
        String     name,
        String     gender,
        String     armSide,
        BigDecimal weightLimit,
        String     ageGroup,
        String     ageCategory,
        String     shift
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId().value(),
                category.getName(),
                category.getGender().name(),
                category.getArmSide().name(),
                category.getWeightLimit(),
                category.getAgeGroup(),
                category.getAgeCategory() == null ? null : category.getAgeCategory().name(),
                category.getShift().name()
        );
    }
}
