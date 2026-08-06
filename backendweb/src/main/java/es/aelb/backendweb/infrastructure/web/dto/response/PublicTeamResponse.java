package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.team.Team;

/**
 * Representación deliberadamente limitada para visitantes. Los datos de contacto
 * del responsable solo se exponen mediante los endpoints de gestión protegidos.
 */
public record PublicTeamResponse(
        String id,
        String name,
        String province,
        String locality,
        boolean hasLogo,
        String logoType
) {
    public static PublicTeamResponse from(Team team) {
        return new PublicTeamResponse(
                team.getId().value(),
                team.getName(),
                team.getProvince(),
                team.getLocality(),
                team.hasLogo(),
                team.getLogoType()
        );
    }
}
