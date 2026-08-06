package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.user.AdminUpdateUserCommand;
import es.aelb.backendweb.application.user.AdminUpdateUserUseCase;
import es.aelb.backendweb.application.user.ChangePasswordCommand;
import es.aelb.backendweb.application.user.ChangePasswordUseCase;
import es.aelb.backendweb.application.user.CreateUserCommand;
import es.aelb.backendweb.application.user.CreateUserUseCase;
import es.aelb.backendweb.application.user.DeleteUserUseCase;
import es.aelb.backendweb.application.user.UpdateProfileCommand;
import es.aelb.backendweb.application.user.UpdateProfileUseCase;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.infrastructure.web.dto.request.AdminUpdateUserRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.ChangePasswordRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.CreateUserRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdateProfileRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase     createUserUseCase;
    private final UpdateProfileUseCase  updateProfileUseCase;
    private final AdminUpdateUserUseCase adminUpdateUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteUserUseCase     deleteUserUseCase;
    private final UserRepository        userRepository;
    private final QuotaRepository       quotaRepository;

    public UserController(
            CreateUserUseCase createUserUseCase,
            UpdateProfileUseCase updateProfileUseCase,
            AdminUpdateUserUseCase adminUpdateUserUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            DeleteUserUseCase deleteUserUseCase,
            UserRepository userRepository,
            QuotaRepository quotaRepository
    ) {
        this.createUserUseCase     = createUserUseCase;
        this.updateProfileUseCase  = updateProfileUseCase;
        this.adminUpdateUserUseCase = adminUpdateUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.deleteUserUseCase     = deleteUserUseCase;
        this.userRepository        = userRepository;
        this.quotaRepository       = quotaRepository;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CreateUserRequest req) {
        String id = createUserUseCase.execute(new CreateUserCommand(
                req.email(), req.password(), req.firstName(),
                req.lastName(), req.dni(), req.phone(),
                req.city(), req.province(), req.nationality(), req.birthDate()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String currentQuota,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Set<String> paidUserIds = Set.copyOf(quotaRepository.findPaidUserIdsByYear(LocalDate.now().getYear()));
        List<UserResponse> all = userRepository.findAll().stream()
                .filter(u -> search.isBlank()
                        || u.getFullName().toLowerCase().contains(search.toLowerCase())
                        || u.getEmail().value().toLowerCase().contains(search.toLowerCase())
                        || u.getDni().value().toLowerCase().contains(search.toLowerCase()))
                .filter(u -> role.isBlank() || u.getRole().name().equalsIgnoreCase(role))
                .filter(u -> currentQuota.isBlank()
                        || (currentQuota.equalsIgnoreCase("PAID") && paidUserIds.contains(u.getId().value()))
                        || (currentQuota.equalsIgnoreCase("PENDING") && !paidUserIds.contains(u.getId().value())))
                .map(UserResponse::from)
                .toList();

        int total = all.size();
        int start = page * size;
        int end   = Math.min(start + size, total);
        List<UserResponse> pageData = (start < total) ? all.subList(start, end) : List.of();

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .header("X-Page", String.valueOf(page))
                .header("X-Page-Size", String.valueOf(size))
                .header("X-Total-Pages", String.valueOf((int) Math.ceil((double) total / size)))
                .body(pageData);
    }

    /**
     * The response contains personal data. An athlete can only retrieve their
     * own profile; GESTOR and ADMIN may retrieve profiles as part of user
     * management.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable String id, Authentication authentication) {
        if (!canReadProfile(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return userRepository.findById(UserId.of(id))
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    static boolean canReadProfile(Authentication authentication, String requestedUserId) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        if (requestedUserId.equals(authentication.getPrincipal())) {
            return true;
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch("ROLE_ADMIN"::equals)
            || authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch("ROLE_GESTOR"::equals);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateProfileRequest req,
            Authentication authentication
    ) {
        String  authenticatedUserId = (String) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!authenticatedUserId.equals(id) && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User updated = updateProfileUseCase.execute(
                new UpdateProfileCommand(id, req.firstName(), req.lastName(), req.phone(), req.birthDate())
        );
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    // ── Admin: full update (profile + role + blocked) ─────────────────────────
    @PutMapping("/{id}/admin")
    public ResponseEntity<UserResponse> adminUpdate(
            @PathVariable String id,
            @Valid @RequestBody AdminUpdateUserRequest req,
            Authentication authentication
    ) {
        String requestingId = (String) authentication.getPrincipal();
        if (id.equals(requestingId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null); // can't self-demote or self-block
        }

        User updated = adminUpdateUserUseCase.execute(new AdminUpdateUserCommand(
                id, req.firstName(), req.lastName(), req.phone(), req.birthDate(),
                req.role(), req.blocked()
        ));
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication
    ) {
        String authenticatedUserId = (String) authentication.getPrincipal();
        if (!authenticatedUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        changePasswordUseCase.execute(
                new ChangePasswordCommand(id, req.currentPassword(), req.newPassword())
        );
        return ResponseEntity.noContent().build();
    }

    // ── Admin: delete user ────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            Authentication authentication
    ) {
        String requestingId = (String) authentication.getPrincipal();
        try {
            deleteUserUseCase.execute(id, requestingId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
