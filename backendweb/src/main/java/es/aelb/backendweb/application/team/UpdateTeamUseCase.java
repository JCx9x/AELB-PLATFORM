package es.aelb.backendweb.application.team;

import es.aelb.backendweb.domain.team.Team;
import es.aelb.backendweb.domain.team.TeamRepository;
import es.aelb.backendweb.domain.team.valueobject.TeamId;

public class UpdateTeamUseCase {

    private final TeamRepository teamRepository;

    public UpdateTeamUseCase(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team execute(UpdateTeamCommand cmd) {
        TeamId id = TeamId.of(cmd.id());

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new Team.NotFoundException(cmd.id()));

        if (teamRepository.existsByNameAndIdNot(cmd.name(), id)) {
            throw new Team.NameAlreadyExistsException(cmd.name());
        }

        team.update(cmd.name(), cmd.responsibleName(), cmd.responsibleLastName(),
                cmd.phone(), cmd.province(), cmd.locality(),
                cmd.logoBase64(), cmd.logoType());
        teamRepository.save(team);
        return team;
    }
}
