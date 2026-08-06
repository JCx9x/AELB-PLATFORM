package es.aelb.backendweb.infrastructure.web.dto.request;

import es.aelb.backendweb.domain.category.ArmSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CalculatePriceRequest(
        @NotNull @Valid @Size(min = 1) List<EntryRequest> entries,
        @Min(0) Integer athleteAge,
        @Min(0) Integer experienceYears,
        @Min(0) Integer goldMedals
) {
    public record EntryRequest(
            @NotBlank String categoryTypeId,
            @NotNull ArmSide armSide
    ) {}
}
