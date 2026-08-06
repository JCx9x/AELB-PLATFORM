package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record UpdateChampionshipRequest(
        @NotBlank @Size(max = 200)
        String name,

        @NotBlank @Size(max = 200)
        String location,

        @NotNull
        LocalDate eventDate,

        @NotNull
        LocalDate registrationDeadline,

        @NotNull @PositiveOrZero
        BigDecimal price,

        String imageKey,

        @Size(max = 2000)
        String description,

        boolean requiresCurrentQuota,

        boolean visible,

        Set<String> categoryIds
) {}
