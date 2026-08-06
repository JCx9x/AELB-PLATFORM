package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.team.CreateTeamCommand;
import es.aelb.backendweb.application.team.CreateTeamUseCase;
import es.aelb.backendweb.application.team.DeleteTeamUseCase;
import es.aelb.backendweb.application.team.UpdateTeamCommand;
import es.aelb.backendweb.application.team.UpdateTeamUseCase;
import es.aelb.backendweb.domain.team.Team;
import es.aelb.backendweb.domain.team.TeamRepository;
import es.aelb.backendweb.domain.team.valueobject.TeamId;
import es.aelb.backendweb.infrastructure.web.dto.request.CreateTeamRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdateTeamRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.TeamResponse;
import es.aelb.backendweb.infrastructure.web.dto.response.PublicTeamResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final CreateTeamUseCase createTeamUseCase;
    private final UpdateTeamUseCase updateTeamUseCase;
    private final DeleteTeamUseCase deleteTeamUseCase;
    private final TeamRepository    teamRepository;

    public TeamController(CreateTeamUseCase createTeamUseCase,
                          UpdateTeamUseCase updateTeamUseCase,
                          DeleteTeamUseCase deleteTeamUseCase,
                          TeamRepository teamRepository) {
        this.createTeamUseCase = createTeamUseCase;
        this.updateTeamUseCase = updateTeamUseCase;
        this.deleteTeamUseCase = deleteTeamUseCase;
        this.teamRepository    = teamRepository;
    }

    @GetMapping
    public ResponseEntity<List<PublicTeamResponse>> findAll() {
        List<PublicTeamResponse> list = teamRepository.findAll().stream()
                .map(PublicTeamResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicTeamResponse> findById(@PathVariable String id) {
        return teamRepository.findById(TeamId.of(id))
                .map(PublicTeamResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Datos completos, únicamente para gestores y administradores. */
    @GetMapping("/admin")
    public ResponseEntity<List<TeamResponse>> findAllForManagement() {
        return ResponseEntity.ok(teamRepository.findAll().stream()
                .map(TeamResponse::from)
                .toList());
    }

    /** Detalle completo, únicamente para gestores y administradores. */
    @GetMapping("/admin/{id}")
    public ResponseEntity<TeamResponse> findByIdForManagement(@PathVariable String id) {
        return teamRepository.findById(TeamId.of(id))
                .map(TeamResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable String id) {
        return teamRepository.findById(TeamId.of(id))
                .filter(Team::hasLogo)
                .map(team -> {
                    byte[] data = Base64.getDecoder().decode(team.getLogoBase64());
                    MediaType mediaType = MediaType.parseMediaType(
                            team.getLogoType() != null ? team.getLogoType() : "image/png"
                    );
                    return ResponseEntity.ok().contentType(mediaType).body(data);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest req) {
        Team created = createTeamUseCase.execute(new CreateTeamCommand(
                req.name(), req.responsibleName(), req.responsibleLastName(),
                req.phone(), req.province(), req.locality(),
                req.logoBase64(), req.logoType()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId().value()).toUri();
        return ResponseEntity.created(location).body(TeamResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateTeamRequest req
    ) {
        Team updated = updateTeamUseCase.execute(new UpdateTeamCommand(
                id, req.name(), req.responsibleName(), req.responsibleLastName(),
                req.phone(), req.province(), req.locality(),
                req.logoBase64(), req.logoType()
        ));
        return ResponseEntity.ok(TeamResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteTeamUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
