package es.aelb.backendweb.application.team;

import es.aelb.backendweb.domain.team.Team;
import es.aelb.backendweb.domain.team.TeamRepository;

public class CreateTeamUseCase {

    private final TeamRepository teamRepository;

    public CreateTeamUseCase(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team execute(CreateTeamCommand cmd) {
        if (teamRepository.existsByName(cmd.name())) {
            throw new Team.NameAlreadyExistsException(cmd.name());
        }

        Team team = Team.create(
                cmd.name(), cmd.responsibleName(), cmd.responsibleLastName(),
                cmd.phone(), cmd.province(), cmd.locality(),
                cmd.logoBase64(), cmd.logoType()
        );
        teamRepository.save(team);
        return team;
    }
}
