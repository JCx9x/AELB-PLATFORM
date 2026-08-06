package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.team.Team;
import es.aelb.backendweb.domain.team.valueobject.TeamId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.TeamJpaEntity;

public class TeamJpaMapper {

    public static TeamJpaEntity toJpa(Team team) {
        return new TeamJpaEntity(
                team.getId().value(),
                team.getName(),
                team.getResponsibleName(),
                team.getResponsibleLastName(),
                team.getPhone(),
                team.getProvince(),
                team.getLocality(),
                team.getLogoBase64(),
                team.getLogoType()
        );
    }

    public static Team toDomain(TeamJpaEntity entity) {
        return Team.reconstitute(
                TeamId.of(entity.getId()),
                entity.getName(),
                entity.getResponsibleName(),
                entity.getResponsibleLastName(),
                entity.getPhone(),
                entity.getProvince(),
                entity.getLocality(),
                entity.getLogoData(),
                entity.getLogoType()
        );
    }

    private TeamJpaMapper() {}
}
