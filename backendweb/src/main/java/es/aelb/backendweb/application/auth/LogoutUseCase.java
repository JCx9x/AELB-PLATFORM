package es.aelb.backendweb.application.auth;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.auth.RefreshToken;
import es.aelb.backendweb.domain.auth.RefreshTokenRepository;

/** Revoca la sesión de refresco actual. Idempotente: un token ausente o ya revocado no es un error. */
public class LogoutUseCase implements UseCase<LogoutCommand, Void> {

    private final RefreshTokenRepository repository;

    public LogoutUseCase(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Void execute(LogoutCommand cmd) {
        if (cmd.rawRefreshToken() == null || cmd.rawRefreshToken().isBlank()) {
            return null;
        }
        repository.findByTokenHash(RefreshTokenCrypto.hash(cmd.rawRefreshToken()))
                .ifPresent(token -> {
                    token.revoke();
                    repository.save(token);
                });
        return null;
    }
}
