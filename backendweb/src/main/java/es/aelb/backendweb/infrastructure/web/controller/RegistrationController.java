package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.registration.CancelRegistrationCommand;
import es.aelb.backendweb.application.registration.CancelRegistrationUseCase;
import es.aelb.backendweb.application.registration.RegisterForChampionshipCommand;
import es.aelb.backendweb.application.registration.RegisterForChampionshipUseCase;
import es.aelb.backendweb.application.registration.UpdatePaymentStatusCommand;
import es.aelb.backendweb.application.registration.UpdatePaymentStatusUseCase;
import es.aelb.backendweb.application.registration.ChangeRegistrationCategoryCommand;
import es.aelb.backendweb.application.registration.ChangeRegistrationCategoryUseCase;
import es.aelb.backendweb.application.registration.CreateStripeRegistrationCheckoutUseCase;
import es.aelb.backendweb.application.registration.CreateStripeChampionshipBasketCheckoutUseCase;
import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.web.dto.request.RegisterForChampionshipRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdatePaymentStatusRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.ChangeRegistrationCategoryRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.AdminRegistrationResponse;
import es.aelb.backendweb.infrastructure.web.dto.response.RegistrationResponse;
import es.aelb.backendweb.infrastructure.pdf.ChampionshipRegistrationsPdfGenerator;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringCategoryJpaRepository;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringUserJpaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class RegistrationController {

    private final RegisterForChampionshipUseCase registerUseCase;
    private final CancelRegistrationUseCase      cancelUseCase;
    private final UpdatePaymentStatusUseCase     updatePaymentUseCase;
    private final ChangeRegistrationCategoryUseCase changeCategoryUseCase;
    private final RegistrationRepository         registrationRepository;
    private final ChampionshipRepository         championshipRepository;
    private final CategoryRepository             categoryRepository;
    private final UserRepository                 userRepository;
    private final CreateStripeRegistrationCheckoutUseCase createStripeCheckoutUseCase;
    private final CreateStripeChampionshipBasketCheckoutUseCase createStripeBasketCheckoutUseCase;
    private final SpringUserJpaRepository userJpaRepository;
    private final SpringCategoryJpaRepository categoryJpaRepository;

    public RegistrationController(
            RegisterForChampionshipUseCase registerUseCase,
            CancelRegistrationUseCase cancelUseCase,
            UpdatePaymentStatusUseCase updatePaymentUseCase,
            ChangeRegistrationCategoryUseCase changeCategoryUseCase,
            RegistrationRepository registrationRepository,
            ChampionshipRepository championshipRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            CreateStripeRegistrationCheckoutUseCase createStripeCheckoutUseCase,
            CreateStripeChampionshipBasketCheckoutUseCase createStripeBasketCheckoutUseCase,
            SpringUserJpaRepository userJpaRepository,
            SpringCategoryJpaRepository categoryJpaRepository
    ) {
        this.registerUseCase        = registerUseCase;
        this.cancelUseCase          = cancelUseCase;
        this.updatePaymentUseCase   = updatePaymentUseCase;
        this.changeCategoryUseCase  = changeCategoryUseCase;
        this.registrationRepository = registrationRepository;
        this.championshipRepository = championshipRepository;
        this.categoryRepository     = categoryRepository;
        this.userRepository         = userRepository;
        this.createStripeCheckoutUseCase = createStripeCheckoutUseCase;
        this.createStripeBasketCheckoutUseCase = createStripeBasketCheckoutUseCase;
        this.userJpaRepository = userJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @PostMapping("/api/championships/{championshipId}/registrations")
    public ResponseEntity<RegistrationResponse> register(
            @PathVariable String championshipId,
            @Valid @RequestBody RegisterForChampionshipRequest req,
            Authentication authentication
    ) {
        String callerId = (String) authentication.getPrincipal();
        String registrationId = registerUseCase.execute(
                new RegisterForChampionshipCommand(callerId, championshipId, req.categoryId(), callerId)
        );

        Registration reg = registrationRepository.findById(registrationId).orElseThrow();
        Championship ch  = championshipRepository.findById(ChampionshipId.of(championshipId)).orElseThrow();
        Category     cat = categoryRepository.findById(CategoryId.of(req.categoryId())).orElseThrow();

        return ResponseEntity.ok(RegistrationResponse.from(reg, ch, cat));
    }

    @PostMapping("/api/championships/{championshipId}/registration-payment/preview")
    public es.aelb.backendweb.infrastructure.web.dto.response.ChampionshipRegistrationPriceResponse previewBasket(
            @PathVariable String championshipId, @Valid @RequestBody es.aelb.backendweb.infrastructure.web.dto.request.ChampionshipRegistrationBasketRequest request, Authentication authentication
    ) {
        var quote = createStripeBasketCheckoutUseCase.quote((String) authentication.getPrincipal(), championshipId, request.categoryIds());
        var items = quote.categories().stream()
                .map(category -> new es.aelb.backendweb.infrastructure.web.dto.response.ChampionshipRegistrationPriceResponse.Item(
                        category.getId().value(), category.getName(), category.getArmSide().name(), category.getWeightLimit()))
                .toList();
        return new es.aelb.backendweb.infrastructure.web.dto.response.ChampionshipRegistrationPriceResponse(quote.total(), "EUR", request.categoryIds(), items);
    }

    @PostMapping("/api/championships/{championshipId}/registration-payment/checkout")
    public es.aelb.backendweb.infrastructure.web.dto.response.StripeCheckoutResponse checkoutBasket(
            @PathVariable String championshipId, @Valid @RequestBody es.aelb.backendweb.infrastructure.web.dto.request.ChampionshipRegistrationBasketRequest request, Authentication authentication
    ) {
        return es.aelb.backendweb.infrastructure.web.dto.response.StripeCheckoutResponse.from(createStripeBasketCheckoutUseCase.create((String) authentication.getPrincipal(), championshipId, request.categoryIds()));
    }

    @DeleteMapping("/api/registrations/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable String id,
            Authentication authentication
    ) {
        String callerId       = (String) authentication.getPrincipal();
        boolean isPrivileged  = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR")
                            || a.getAuthority().equals("ROLE_ADMIN"));

        cancelUseCase.execute(new CancelRegistrationCommand(id, callerId, isPrivileged));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/{userId}/registrations")
    public ResponseEntity<List<RegistrationResponse>> findByUser(
            @PathVariable String userId,
            Authentication authentication
    ) {
        String callerId      = (String) authentication.getPrincipal();
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR")
                            || a.getAuthority().equals("ROLE_ADMIN"));

        if (!callerId.equals(userId) && !isPrivileged) {
            return ResponseEntity.status(403).build();
        }

        List<Registration> registrations = registrationRepository.findByUser(UserId.of(userId));
        List<RegistrationResponse> responses = registrations.stream()
                .map(reg -> {
                    Optional<Championship> ch  = championshipRepository.findById(reg.getChampionshipId());
                    Optional<Category>     cat = categoryRepository.findById(CategoryId.of(reg.getCategoryId()));
                    if (ch.isEmpty() || cat.isEmpty()) return null;
                    return RegistrationResponse.from(reg, ch.get(), cat.get());
                })
                .filter(r -> r != null)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/championships/{championshipId}/registrations")
    public ResponseEntity<List<AdminRegistrationResponse>> findByChampionship(
            @PathVariable String championshipId,
            Authentication authentication
    ) {
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR")
                            || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isPrivileged) {
            return ResponseEntity.status(403).build();
        }

        List<Registration> registrations = registrationRepository.findByChampionship(
                ChampionshipId.of(championshipId)
        );

        List<AdminRegistrationResponse> responses = registrations.stream()
                .map(reg -> {
                    Optional<User>     user = userRepository.findById(reg.getUserId());
                    Optional<Category> cat  = categoryRepository.findById(CategoryId.of(reg.getCategoryId()));
                    if (user.isEmpty() || cat.isEmpty()) return null;
                    return AdminRegistrationResponse.from(reg, user.get(), cat.get());
                })
                .filter(r -> r != null)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/api/championships/{championshipId}/registrations/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadRegistrationsPdf(@PathVariable String championshipId, Authentication authentication) {
        boolean isPrivileged = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR") || a.getAuthority().equals("ROLE_ADMIN"));
        if (!isPrivileged) return ResponseEntity.status(403).build();
        Championship championship = championshipRepository.findById(ChampionshipId.of(championshipId)).orElseThrow();
        List<Registration> registrations = registrationRepository.findByChampionship(ChampionshipId.of(championshipId));
        Set<String> userIds = registrations.stream().map(registration -> registration.getUserId().value()).collect(Collectors.toSet());
        Set<String> categoryIds = registrations.stream().map(Registration::getCategoryId).collect(Collectors.toSet());
        Map<String, es.aelb.backendweb.infrastructure.persistence.jpa.entity.UserJpaEntity> users = userJpaRepository.findAllById(userIds).stream().collect(Collectors.toMap(entity -> entity.getId(), entity -> entity));
        Map<String, es.aelb.backendweb.infrastructure.persistence.jpa.entity.CategoryJpaEntity> categories = categoryJpaRepository.findAllById(categoryIds).stream().collect(Collectors.toMap(entity -> entity.getId(), entity -> entity));
        List<ChampionshipRegistrationsPdfGenerator.Entry> entries = registrations.stream()
                .map(registration -> { var user = users.get(registration.getUserId().value()); var category = categories.get(registration.getCategoryId()); return user == null || category == null ? null : new ChampionshipRegistrationsPdfGenerator.Entry(category.getName(), user.getFirstName(), user.getLastName(), user.getDni()); })
                .filter(java.util.Objects::nonNull).toList();
        byte[] pdf = new ChampionshipRegistrationsPdfGenerator().generate(championship.getName(), championship.getEventDate(), entries);
        String filename = "inscritos-" + championship.getName().replaceAll("[^a-zA-Z0-9_-]", "-") + ".pdf";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private").contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @PutMapping("/api/registrations/{id}/payment")
    public ResponseEntity<Void> updatePayment(
            @PathVariable String id,
            @Valid @RequestBody UpdatePaymentStatusRequest req,
            Authentication authentication
    ) {
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR")
                            || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isPrivileged) {
            return ResponseEntity.status(403).build();
        }

        updatePaymentUseCase.execute(new UpdatePaymentStatusCommand(id, req.status()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/registrations/{id}/category")
    public ResponseEntity<Void> changeCategory(
            @PathVariable String id,
            @Valid @RequestBody ChangeRegistrationCategoryRequest req,
            Authentication authentication
    ) {
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR")
                            || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isPrivileged) {
            return ResponseEntity.status(403).build();
        }

        changeCategoryUseCase.execute(new ChangeRegistrationCategoryCommand(id, req.categoryId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/registrations/{id}/checkout")
    public es.aelb.backendweb.infrastructure.web.dto.response.StripeCheckoutResponse createStripeCheckout(
            @PathVariable String id, Authentication authentication
    ) {
        return es.aelb.backendweb.infrastructure.web.dto.response.StripeCheckoutResponse.from(
                createStripeCheckoutUseCase.execute(id, (String) authentication.getPrincipal())
        );
    }
}
