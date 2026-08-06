package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.ChampionshipJpaEntity;

public class ChampionshipJpaMapper {

    private ChampionshipJpaMapper() {}

    public static ChampionshipJpaEntity toJpa(Championship championship) {
        ChampionshipJpaEntity entity = new ChampionshipJpaEntity();
        entity.setId(championship.getId().value());
        entity.setName(championship.getName());
        entity.setLocation(championship.getLocation());
        entity.setEventDate(championship.getEventDate());
        entity.setRegistrationDeadline(championship.getRegistrationDeadline());
        entity.setPrice(championship.getPrice());
        entity.setVisible(championship.isVisible());
        entity.setImageUrl(championship.getImageUrl());
        entity.setDescription(championship.getDescription());
        entity.setRequiresCurrentQuota(championship.requiresCurrentQuota());
        entity.setCreatedBy(championship.getCreatedBy().value());
        entity.setCreatedAt(championship.getCreatedAt());
        entity.setUpdatedAt(championship.getUpdatedAt());
        entity.getCategoryIds().addAll(championship.getCategoryIds());
        return entity;
    }

    public static Championship toDomain(ChampionshipJpaEntity entity) {
        return Championship.reconstitute(
                ChampionshipId.of(entity.getId()),
                entity.getName(),
                entity.getLocation(),
                entity.getEventDate(),
                entity.getRegistrationDeadline(),
                entity.getPrice(),
                entity.isVisible(),
                entity.getImageUrl(),
                entity.getDescription(),
                entity.isRequiresCurrentQuota(),
                UserId.of(entity.getCreatedBy()),
                entity.getCategoryIds(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
