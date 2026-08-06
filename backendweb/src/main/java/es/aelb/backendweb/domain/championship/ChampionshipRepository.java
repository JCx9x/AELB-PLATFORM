package es.aelb.backendweb.domain.championship;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;

import java.util.List;
import java.util.Optional;

public interface ChampionshipRepository {

    void               save(Championship championship);
    Optional<Championship> findById(ChampionshipId id);
    List<Championship> findAll();
    List<Championship> findVisible();
    void               delete(ChampionshipId id);
}
