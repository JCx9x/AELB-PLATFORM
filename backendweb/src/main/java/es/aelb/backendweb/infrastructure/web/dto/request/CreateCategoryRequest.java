package es.aelb.backendweb.infrastructure.web.dto.request;

import es.aelb.backendweb.domain.category.ArmSide;
import es.aelb.backendweb.domain.category.Gender;
import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import es.aelb.backendweb.domain.category.Shift;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCategoryRequest(
        /** Compatibilidad con clientes anteriores; el servidor genera el título. */
        @Size(max = 100)
        String name,

        @NotNull
        Gender gender,

        @NotNull
        ArmSide armSide,

        BigDecimal weightLimit,

        @Size(max = 50)
        String ageGroup,

        CompetitionAgeCategory ageCategory,

        @NotNull
        Shift shift
) {}
