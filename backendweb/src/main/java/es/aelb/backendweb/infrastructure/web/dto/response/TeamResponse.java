package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.team.Team;

public record TeamResponse(
        String  id,
        String  name,
        String  responsibleName,
        String  responsibleLastName,
        String  phone,
        String  province,
        String  locality,
        boolean hasLogo,
        String  logoType
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId().value(),
                team.getName(),
                team.getResponsibleName(),
                team.getResponsibleLastName(),
                team.getPhone(),
                team.getProvince(),
                team.getLocality(),
                team.hasLogo(),
                team.getLogoType()
        );
    }
}
