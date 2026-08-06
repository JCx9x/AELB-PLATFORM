package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.*;
import es.aelb.backendweb.domain.pricing.checkout.CheckoutLineItem;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.pricing.engine.*;
import es.aelb.backendweb.domain.shared.exception.DomainException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula el precio, valida el basket y crea un checkout listo para pagar.
 * El checkout incluye líneas de concepto, total, metadatos y expira en 30 min.
 */
public class PrepareCheckoutUseCase implements UseCase<PrepareCheckoutCommand, RegistrationCheckout> {

    private final PricingConfigRepository       pricingConfigRepository;
    private final RegistrationCheckoutRepository checkoutRepository;
    private final PricingEngine                 pricingEngine;

    public PrepareCheckoutUseCase(
            PricingConfigRepository pricingConfigRepository,
            RegistrationCheckoutRepository checkoutRepository,
            PricingEngine pricingEngine
    ) {
        this.pricingConfigRepository = pricingConfigRepository;
        this.checkoutRepository      = checkoutRepository;
        this.pricingEngine           = pricingEngine;
    }

    @Override
    public RegistrationCheckout execute(PrepareCheckoutCommand cmd) {
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

        PriceCalculationResult result = pricingEngine.calculate(config, basket, athlete);

        if (!result.isValid()) {
            throw new CheckoutValidationException(result.getValidationErrors());
        }

        List<CheckoutLineItem> lineItems = result.getLineItems().stream()
                .map(li -> CheckoutLineItem.of(li.concept(), li.amount(), li.ruleId()))
                .toList();

        Map<String, String> metadata = buildMetadata(cmd, entries);

        RegistrationCheckout checkout = RegistrationCheckout.create(
                cmd.userId(),
                cmd.championshipId(),
                lineItems,
                result.getTotal(),
                result.getCurrency(),
                metadata,
                null
        );

        checkoutRepository.save(checkout);
        return checkout;
    }

    private Map<String, String> buildMetadata(PrepareCheckoutCommand cmd, List<RegistrationEntry> entries) {
        Map<String, String> meta = new HashMap<>();
        meta.put("userId",         cmd.userId());
        meta.put("championshipId", cmd.championshipId());
        meta.put("entryCount",     String.valueOf(entries.size()));
        meta.put("categoryTypes",  String.join(",", entries.stream().map(RegistrationEntry::categoryTypeId).distinct().toList()));
        meta.put("armSides",       String.join(",", entries.stream().map(e -> e.armSide().name()).distinct().toList()));
        if (cmd.athleteAge() != null) meta.put("athleteAge", String.valueOf(cmd.athleteAge()));
        if (cmd.extraMetadata() != null) meta.putAll(cmd.extraMetadata());
        return meta;
    }

    public static final class CheckoutValidationException extends DomainException {
        public CheckoutValidationException(List<String> errors) {
            super("El basket no es válido para inscripción: " + String.join("; ", errors));
        }
    }
}
