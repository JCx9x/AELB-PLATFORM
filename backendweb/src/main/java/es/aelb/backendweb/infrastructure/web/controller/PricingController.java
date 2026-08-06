package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.pricing.*;
import es.aelb.backendweb.domain.pricing.PricingConfig;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.engine.PriceCalculationResult;
import es.aelb.backendweb.infrastructure.web.dto.request.CalculatePriceRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.PrepareCheckoutRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.PricingConfigRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.PriceCalculationResponse;
import es.aelb.backendweb.infrastructure.web.dto.response.PricingConfigResponse;
import es.aelb.backendweb.infrastructure.web.dto.response.RegistrationCheckoutResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final SavePricingConfigUseCase    savePricingConfigUseCase;
    private final GetPricingConfigUseCase     getPricingConfigUseCase;
    private final CalculatePriceUseCase       calculatePriceUseCase;
    private final PrepareCheckoutUseCase      prepareCheckoutUseCase;
    private final ApplyPricingTemplateUseCase applyPricingTemplateUseCase;

    public PricingController(
            SavePricingConfigUseCase savePricingConfigUseCase,
            GetPricingConfigUseCase getPricingConfigUseCase,
            CalculatePriceUseCase calculatePriceUseCase,
            PrepareCheckoutUseCase prepareCheckoutUseCase,
            ApplyPricingTemplateUseCase applyPricingTemplateUseCase
    ) {
        this.savePricingConfigUseCase    = savePricingConfigUseCase;
        this.getPricingConfigUseCase     = getPricingConfigUseCase;
        this.calculatePriceUseCase       = calculatePriceUseCase;
        this.prepareCheckoutUseCase      = prepareCheckoutUseCase;
        this.applyPricingTemplateUseCase = applyPricingTemplateUseCase;
    }

    // ── Configuración de precios ─────────────────────────────────────────

    /** Crea o reemplaza la configuración de precios de un campeonato. */
    @PutMapping("/configs/{championshipId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<PricingConfigResponse> saveConfig(
            @PathVariable String championshipId,
            @Valid @RequestBody PricingConfigRequest request
    ) {
        savePricingConfigUseCase.execute(toCommand(championshipId, request));
        PricingConfig config = getPricingConfigUseCase.execute(championshipId);
        return ResponseEntity
                .created(URI.create("/api/pricing/configs/" + championshipId))
                .body(PricingConfigResponse.from(config));
    }

    /** Obtiene la configuración de precios de un campeonato. */
    @GetMapping("/configs/{championshipId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<PricingConfigResponse> getConfig(@PathVariable String championshipId) {
        return ResponseEntity.ok(PricingConfigResponse.from(getPricingConfigUseCase.execute(championshipId)));
    }

    /**
     * Aplica un template predefinido de reglas a un campeonato.
     * Templates disponibles: AELB_STANDARD
     */
    @PostMapping("/configs/{championshipId}/apply-template/{templateName}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<PricingConfigResponse> applyTemplate(
            @PathVariable String championshipId,
            @PathVariable String templateName
    ) {
        applyPricingTemplateUseCase.execute(
                new ApplyPricingTemplateUseCase.Command(championshipId, templateName)
        );
        return ResponseEntity.ok(PricingConfigResponse.from(getPricingConfigUseCase.execute(championshipId)));
    }

    // ── Cálculo y checkout ───────────────────────────────────────────────

    /** Calcula el precio sin crear checkout. Útil para mostrar el precio antes de pagar. */
    @PostMapping("/configs/{championshipId}/calculate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PriceCalculationResponse> calculate(
            @PathVariable String championshipId,
            @Valid @RequestBody CalculatePriceRequest request
    ) {
        CalculatePriceCommand cmd = new CalculatePriceCommand(
                championshipId,
                request.entries().stream()
                        .map(e -> new CalculatePriceCommand.EntryData(e.categoryTypeId(), e.armSide()))
                        .toList(),
                request.athleteAge(),
                request.experienceYears(),
                request.goldMedals()
        );
        PriceCalculationResult result   = calculatePriceUseCase.execute(cmd);
        PriceCalculationResponse response = PriceCalculationResponse.from(result);
        return result.isValid()
                ? ResponseEntity.ok(response)
                : ResponseEntity.unprocessableEntity().body(response);
    }

    /**
     * Crea un checkout de pago.
     * El checkout expira en 30 minutos. Una vez creado, se pasa al proveedor de pago.
     */
    @PostMapping("/configs/{championshipId}/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegistrationCheckoutResponse> prepareCheckout(
            @PathVariable String championshipId,
            @Valid @RequestBody PrepareCheckoutRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        PrepareCheckoutCommand cmd = new PrepareCheckoutCommand(
                principal.getUsername(),
                championshipId,
                request.entries().stream()
                        .map(e -> new PrepareCheckoutCommand.EntryData(e.categoryTypeId(), e.armSide()))
                        .toList(),
                request.athleteAge(),
                request.experienceYears(),
                request.goldMedals(),
                request.extraMetadata()
        );
        RegistrationCheckout checkout = prepareCheckoutUseCase.execute(cmd);
        return ResponseEntity
                .created(URI.create("/api/pricing/checkouts/" + checkout.getId()))
                .body(RegistrationCheckoutResponse.from(checkout));
    }

    // ── Mappers ──────────────────────────────────────────────────────────

    private SavePricingConfigCommand toCommand(String championshipId, PricingConfigRequest req) {
        return new SavePricingConfigCommand(
                championshipId,
                req.maxCategoriesPerArm(),
                req.maxTotalEntries(),
                req.currency(),
                req.rules().stream()
                        .map(r -> new SavePricingConfigCommand.RuleData(
                                r.name(), r.description(), r.priority(), r.active(),
                                r.conditions().stream()
                                        .map(c -> new SavePricingConfigCommand.ConditionData(c.type(), c.value()))
                                        .toList(),
                                new SavePricingConfigCommand.EffectData(r.effect().type(), r.effect().amount(), r.effect().description())
                        ))
                        .toList(),
                req.categoryRestrictions().stream()
                        .map(r -> new SavePricingConfigCommand.RestrictionData(
                                r.categoryTypeId(),
                                r.ageMin(), r.ageMax(),
                                r.maxExperienceYears(), r.maxGoldMedals(),
                                r.incompatibleWith()        != null ? r.incompatibleWith()        : Set.of(),
                                r.requiresCombinationWith() != null ? r.requiresCombinationWith() : Set.of()
                        ))
                        .toList()
        );
    }
}
