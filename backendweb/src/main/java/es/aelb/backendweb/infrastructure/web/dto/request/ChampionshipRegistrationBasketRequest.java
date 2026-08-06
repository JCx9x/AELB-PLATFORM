package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChampionshipRegistrationBasketRequest(@NotEmpty @Size(max = 4) List<String> categoryIds) {}
