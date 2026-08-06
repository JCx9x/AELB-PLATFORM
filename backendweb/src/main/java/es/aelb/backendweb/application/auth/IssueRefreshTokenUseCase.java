package es.aelb.backendweb.application.auth;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.auth.RefreshToken;
import es.aelb.backendweb.domain.auth.RefreshTokenRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.Duration;
import java.time.LocalDateTime;

/** Emite una nueva sesión de refresco para un usuario ya autenticado (login o rotación). */
public class IssueRefreshTokenUseCase implements UseCase<IssueRefreshTokenCommand, IssuedRefreshToken> {

    private final RefreshTokenRepository repository;
    private final Duration               ttl;

    public IssueRefreshTokenUseCase(RefreshTokenRepository repository, Duration ttl) {
        this.repository = repository;
        this.ttl        = ttl;
    }

    @Override
    public IssuedRefreshToken execute(IssueRefreshTokenCommand cmd) {
        String rawToken = RefreshTokenCrypto.generateRawToken();
        LocalDateTime expiresAt = LocalDateTime.now().plus(ttl);

        RefreshToken token = RefreshToken.issue(
                UserId.of(cmd.userId()),
                RefreshTokenCrypto.hash(rawToken),
                expiresAt
        );
        repository.save(token);

        return new IssuedRefreshToken(rawToken, expiresAt);
    }
}
