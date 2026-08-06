package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.ChampionshipAgeCategoryPrice;
import es.aelb.backendweb.domain.pricing.ChampionshipAgeCategoryPriceRepository;
import es.aelb.backendweb.infrastructure.web.dto.request.ChampionshipAgeCategoryPricesRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.ChampionshipAgeCategoryPriceResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/championships/{championshipId}/category-prices")
public class ChampionshipPricingController {
    private final ChampionshipAgeCategoryPriceRepository repository;
    public ChampionshipPricingController(ChampionshipAgeCategoryPriceRepository repository) { this.repository = repository; }

    @GetMapping
    public ResponseEntity<List<ChampionshipAgeCategoryPriceResponse>> get(@PathVariable String championshipId) {
        return ResponseEntity.ok(complete(repository.findByChampionshipId(ChampionshipId.of(championshipId))).stream()
                .map(ChampionshipAgeCategoryPriceResponse::from).toList());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<List<ChampionshipAgeCategoryPriceResponse>> put(
            @PathVariable String championshipId, @Valid @RequestBody ChampionshipAgeCategoryPricesRequest request) {
        List<ChampionshipAgeCategoryPrice> prices = request.prices().stream()
                .map(p -> new ChampionshipAgeCategoryPrice(p.ageCategory(), p.pricePerArm(), p.combinationPrice())).toList();
        if (prices.stream().map(ChampionshipAgeCategoryPrice::ageCategory).collect(java.util.stream.Collectors.toSet()).size() != CompetitionAgeCategory.values().length)
            throw new IllegalArgumentException("Debe configurarse exactamente un precio para cada categoría WAF.");
        repository.replaceForChampionship(ChampionshipId.of(championshipId), prices);
        return ResponseEntity.ok(prices.stream().map(ChampionshipAgeCategoryPriceResponse::from).toList());
    }

    private static List<ChampionshipAgeCategoryPrice> complete(List<ChampionshipAgeCategoryPrice> configured) {
        Map<CompetitionAgeCategory, ChampionshipAgeCategoryPrice> byCategory = new EnumMap<>(CompetitionAgeCategory.class);
        configured.forEach(price -> byCategory.put(price.ageCategory(), price));
        return Arrays.stream(CompetitionAgeCategory.values()).map(category -> byCategory.getOrDefault(category, ChampionshipAgeCategoryPrice.defaultFor(category))).toList();
    }
}
