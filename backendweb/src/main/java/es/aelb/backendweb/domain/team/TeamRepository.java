package es.aelb.backendweb.domain.team;

import es.aelb.backendweb.domain.team.valueobject.TeamId;

import java.util.List;
import java.util.Optional;

public interface TeamRepository {
    void               save(Team team);
    Optional<Team>     findById(TeamId id);
    boolean            existsByName(String name);
    boolean            existsByNameAndIdNot(String name, TeamId excludeId);
    List<Team>         findAll();
    void               delete(TeamId id);
}
