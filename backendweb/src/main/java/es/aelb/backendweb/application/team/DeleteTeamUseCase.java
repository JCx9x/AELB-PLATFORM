package es.aelb.backendweb.application.team;

import es.aelb.backendweb.domain.team.Team;
import es.aelb.backendweb.domain.team.TeamRepository;
import es.aelb.backendweb.domain.team.valueobject.TeamId;

public class DeleteTeamUseCase {

    private final TeamRepository teamRepository;

    public DeleteTeamUseCase(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public void execute(String id) {
        TeamId teamId = TeamId.of(id);
        if (!teamRepository.findById(teamId).isPresent()) {
            throw new Team.NotFoundException(id);
        }
        teamRepository.delete(teamId);
    }
}
