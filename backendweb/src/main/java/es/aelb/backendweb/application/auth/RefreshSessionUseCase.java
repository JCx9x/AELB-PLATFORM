package es.aelb.backendweb.application.auth;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.auth.RefreshToken;
import es.aelb.backendweb.domain.auth.RefreshTokenRepository;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;

/**
 * Rota una sesión de refresco: valida el token presentado, lo revoca y emite
 * uno nuevo junto con los datos necesarios para acuñar un access token fresco.
 *
 * Detección de robo: si el token presentado ya está revocado (es decir, ya
 * fue usado en una rotación anterior), significa que hay dos copias del
 * mismo token circulando — se revocan TODAS las sesiones del usuario.
 */
public class RefreshSessionUseCase implements UseCase<RefreshSessionCommand, RefreshSessionResult> {

    private final RefreshTokenRepository   refreshTokenRepository;
    private final UserRepository           userRepository;
    private final IssueRefreshTokenUseCase issueRefreshTokenUseCase;

    public RefreshSessionUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            IssueRefreshTokenUseCase issueRefreshTokenUseCase
    ) {
        this.refreshTokenRepository   = refreshTokenRepository;
        this.userRepository           = userRepository;
        this.issueRefreshTokenUseCase = issueRefreshTokenUseCase;
    }

    @Override
    public RefreshSessionResult execute(RefreshSessionCommand cmd) {
        String hash = RefreshTokenCrypto.hash(cmd.rawRefreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(token.getUserId());
            throw new RefreshTokenReusedException();
        }
        if (token.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);
        if (user.isBlocked()) {
            refreshTokenRepository.revokeAllForUser(user.getId());
            throw new InvalidRefreshTokenException();
        }

        token.revoke();
        refreshTokenRepository.save(token);

        IssuedRefreshToken next = issueRefreshTokenUseCase.execute(
                new IssueRefreshTokenCommand(user.getId().value())
        );

        return new RefreshSessionResult(
                user.getId().value(),
                user.getEmail().value(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName(),
                user.getTokenVersion(),
                next.rawToken(),
                next.expiresAt()
        );
    }

    public static final class InvalidRefreshTokenException extends DomainException {
        public InvalidRefreshTokenException() {
            super("Sesión de refresco inválida o caducada");
        }
    }

    public static final class RefreshTokenReusedException extends DomainException {
        public RefreshTokenReusedException() {
            super("Reutilización de sesión de refresco detectada; todas las sesiones han sido revocadas");
        }
    }
}
