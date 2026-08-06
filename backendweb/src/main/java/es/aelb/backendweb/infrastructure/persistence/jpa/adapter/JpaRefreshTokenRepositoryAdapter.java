package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.auth.RefreshToken;
import es.aelb.backendweb.domain.auth.RefreshTokenRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RefreshTokenJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringRefreshTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringRefreshTokenJpaRepository repository;

    public JpaRefreshTokenRepositoryAdapter(SpringRefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        repository.save(toJpa(refreshToken));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public void revokeAllForUser(UserId userId) {
        repository.revokeAllForUser(userId.value(), LocalDateTime.now());
    }

    private RefreshTokenJpaEntity toJpa(RefreshToken t) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(t.getId());
        entity.setUserId(t.getUserId().value());
        entity.setTokenHash(t.getTokenHash());
        entity.setExpiresAt(t.getExpiresAt());
        entity.setRevokedAt(t.getRevokedAt());
        entity.setCreatedAt(t.getCreatedAt());
        return entity;
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.reconstitute(
                e.getId(), UserId.of(e.getUserId()), e.getTokenHash(),
                e.getExpiresAt(), e.getCreatedAt(), e.getRevokedAt()
        );
    }
}
