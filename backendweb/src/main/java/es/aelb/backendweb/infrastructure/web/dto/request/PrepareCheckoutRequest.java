package es.aelb.backendweb.infrastructure.web.dto.request;

import es.aelb.backendweb.domain.category.ArmSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

public record PrepareCheckoutRequest(
        @NotNull @Valid @Size(min = 1) List<EntryRequest> entries,
        @Min(0) Integer athleteAge,
        @Min(0) Integer experienceYears,
        @Min(0) Integer goldMedals,
        Map<String, String> extraMetadata
) {
    public record EntryRequest(
            @NotBlank String categoryTypeId,
            @NotNull ArmSide armSide
    ) {}
}
