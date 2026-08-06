package es.aelb.backendweb.application.category;

import es.aelb.backendweb.domain.category.ArmSide;
import es.aelb.backendweb.domain.category.Gender;
import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import es.aelb.backendweb.domain.category.Shift;

import java.math.BigDecimal;

public record UpdateCategoryCommand(
        String     categoryId,
        Gender     gender,
        ArmSide    armSide,
        BigDecimal weightLimit,
        String     ageGroup,
        CompetitionAgeCategory ageCategory,
        Shift      shift
) {}
