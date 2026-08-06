package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterForChampionshipRequest(
        @NotBlank String categoryId
) {}
