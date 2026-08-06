package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.category.CompetitionAgeCategory;

public record CompetitionAgeCategoryResponse(
        String id,
        String displayName,
        String denomination,
        Integer minimumAge,
        Integer maximumAge,
        String shift
) {
    public static CompetitionAgeCategoryResponse from(CompetitionAgeCategory category) {
        return new CompetitionAgeCategoryResponse(category.name(), category.getDisplayName(), category.getDenomination(),
                category.getMinimumAge(), category.getMaximumAge(), category.getShift().name());
    }
}
