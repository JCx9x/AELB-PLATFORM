package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.quota.*;
import es.aelb.backendweb.domain.quota.*;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.web.dto.request.AnnualQuotaPricesRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.AnnualQuotaPriceResponse;
import es.aelb.backendweb.infrastructure.web.dto.response.QuotaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/quotas")
public class QuotaController {
    private final SaveAnnualQuotaPricesUseCase savePrices;
    private final GetOrCreateUserQuotaUseCase getOrCreateQuota;
    private final MarkQuotaPaidUseCase markPaid;
    private final CreateStripeQuotaCheckoutUseCase createStripeCheckout;
    private final AnnualQuotaPriceRepository prices;
    private final QuotaRepository quotas;
    public QuotaController(SaveAnnualQuotaPricesUseCase savePrices, GetOrCreateUserQuotaUseCase getOrCreateQuota, MarkQuotaPaidUseCase markPaid, CreateStripeQuotaCheckoutUseCase createStripeCheckout, AnnualQuotaPriceRepository prices, QuotaRepository quotas) { this.savePrices = savePrices; this.getOrCreateQuota = getOrCreateQuota; this.markPaid = markPaid; this.createStripeCheckout = createStripeCheckout; this.prices = prices; this.quotas = quotas; }

    @GetMapping("/prices/{year}")
    public List<AnnualQuotaPriceResponse> getPrices(@PathVariable int year) { return prices.findByYear(year).stream().map(AnnualQuotaPriceResponse::from).toList(); }

    @PutMapping("/prices/{year}")
    public List<AnnualQuotaPriceResponse> savePrices(@PathVariable int year, @Valid @RequestBody AnnualQuotaPricesRequest request) {
        Map<AgeCategory, BigDecimal> values = new EnumMap<>(AgeCategory.class);
        request.prices().forEach(p -> values.put(p.ageCategory(), p.amount()));
        return savePrices.execute(year, values).stream().map(AnnualQuotaPriceResponse::from).toList();
    }

    /** Genera de forma perezosa la cuota del usuario, calculando su edad al 31 de diciembre. */
    @PostMapping("/my/{year}")
    public QuotaResponse getOrCreateMyQuota(@PathVariable int year, Authentication authentication) { return QuotaResponse.from(getOrCreateQuota.execute((String) authentication.getPrincipal(), year)); }

    @PostMapping("/my/{year}/checkout")
    public es.aelb.backendweb.infrastructure.web.dto.response.StripeCheckoutResponse createCheckout(@PathVariable int year, Authentication authentication) { return es.aelb.backendweb.infrastructure.web.dto.response.StripeCheckoutResponse.from(createStripeCheckout.execute((String) authentication.getPrincipal(), year)); }

    @GetMapping("/my")
    public List<QuotaResponse> getMyQuotas(Authentication authentication) { return quotas.findByUser(UserId.of((String) authentication.getPrincipal())).stream().map(QuotaResponse::from).toList(); }

    @PutMapping("/users/{userId}/{year}/payment")
    public QuotaResponse markPaid(@PathVariable String userId, @PathVariable int year, Authentication authentication) {
        getOrCreateQuota.execute(userId, year);
        return QuotaResponse.from(markPaid.execute(userId, year, (String) authentication.getPrincipal()));
    }

    @GetMapping("/users/{userId}")
    public List<QuotaResponse> getUserQuotas(@PathVariable String userId) { return quotas.findByUser(UserId.of(userId)).stream().map(QuotaResponse::from).toList(); }
}
