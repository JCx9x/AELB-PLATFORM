package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.*;
import es.aelb.backendweb.domain.pricing.engine.*;

import java.util.List;

public class CalculatePriceUseCase implements UseCase<CalculatePriceCommand, PriceCalculationResult> {

    private final PricingConfigRepository pricingConfigRepository;
    private final PricingEngine           pricingEngine;

    public CalculatePriceUseCase(
            PricingConfigRepository pricingConfigRepository,
            PricingEngine pricingEngine
    ) {
        this.pricingConfigRepository = pricingConfigRepository;
        this.pricingEngine           = pricingEngine;
    }

    @Override
    public PriceCalculationResult execute(CalculatePriceCommand cmd) {
        PricingConfig config = pricingConfigRepository
                .findByChampionshipId(ChampionshipId.of(cmd.championshipId()))
                .orElseThrow(() -> new PricingException.ConfigNotFoundException(cmd.championshipId()));

        List<RegistrationEntry> entries = cmd.entries().stream()
                .map(e -> RegistrationEntry.of(e.categoryTypeId(), e.armSide()))
                .toList();

        RegistrationBasket basket  = RegistrationBasket.of(entries);
        AthleteContext     athlete = AthleteContext.of(
                cmd.athleteAge()       != null ? cmd.athleteAge()       : Integer.MAX_VALUE,
                cmd.experienceYears()  != null ? cmd.experienceYears()  : 0,
                cmd.goldMedals()       != null ? cmd.goldMedals()       : 0
        );

        return pricingEngine.calculate(config, basket, athlete);
    }
}
