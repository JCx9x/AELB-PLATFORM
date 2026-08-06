package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.championship.CreateChampionshipCommand;
import es.aelb.backendweb.application.championship.CreateChampionshipUseCase;
import es.aelb.backendweb.application.championship.DeleteChampionshipUseCase;
import es.aelb.backendweb.application.championship.UpdateChampionshipCommand;
import es.aelb.backendweb.application.championship.UpdateChampionshipUseCase;
import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.infrastructure.storage.PresignedUrlService;
import es.aelb.backendweb.infrastructure.web.dto.request.CreateChampionshipRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdateChampionshipRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.ChampionshipResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/championships")
public class ChampionshipController {

    private final CreateChampionshipUseCase createChampionshipUseCase;
    private final UpdateChampionshipUseCase updateChampionshipUseCase;
    private final DeleteChampionshipUseCase deleteChampionshipUseCase;
    private final ChampionshipRepository    championshipRepository;
    private final PresignedUrlService       presignedUrlService;

    public ChampionshipController(
            CreateChampionshipUseCase createChampionshipUseCase,
            UpdateChampionshipUseCase updateChampionshipUseCase,
            DeleteChampionshipUseCase deleteChampionshipUseCase,
            ChampionshipRepository    championshipRepository,
            PresignedUrlService       presignedUrlService
    ) {
        this.createChampionshipUseCase = createChampionshipUseCase;
        this.updateChampionshipUseCase = updateChampionshipUseCase;
        this.deleteChampionshipUseCase = deleteChampionshipUseCase;
        this.championshipRepository    = championshipRepository;
        this.presignedUrlService       = presignedUrlService;
    }

    @GetMapping
    public ResponseEntity<List<ChampionshipResponse>> findVisible() {
        return ResponseEntity.ok(championshipRepository.findVisible().stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ChampionshipResponse>> findAll() {
        return ResponseEntity.ok(championshipRepository.findAll().stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChampionshipResponse> findById(@PathVariable String id) {
        return championshipRepository.findById(ChampionshipId.of(id))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ChampionshipResponse> create(
            @Valid @RequestBody CreateChampionshipRequest req,
            Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        Championship created = createChampionshipUseCase.execute(new CreateChampionshipCommand(
                req.name(), req.location(),
                req.eventDate(), req.registrationDeadline(),
                req.price(), req.imageKey(), req.description(),
                req.requiresCurrentQuota(), userId, req.categoryIds()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId().value()).toUri();
        return ResponseEntity.created(location).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChampionshipResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateChampionshipRequest req
    ) {
        Championship updated = updateChampionshipUseCase.execute(new UpdateChampionshipCommand(
                id, req.name(), req.location(),
                req.eventDate(), req.registrationDeadline(),
                req.price(), req.imageKey(), req.description(),
                req.requiresCurrentQuota(), req.visible(), req.categoryIds()
        ));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteChampionshipUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    private ChampionshipResponse toResponse(Championship c) {
        return ChampionshipResponse.from(c, presignedUrlService.resolveUrl(c.getImageUrl()));
    }
}
