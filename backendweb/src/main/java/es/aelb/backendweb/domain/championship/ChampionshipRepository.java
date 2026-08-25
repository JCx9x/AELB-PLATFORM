package es.aelb.backendweb.domain.championship;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface ChampionshipRepository {

    void               save(Championship championship);
    Optional<Championship> findById(ChampionshipId id);
    Optional<Championship> findVisibleById(ChampionshipId id);
    List<Championship> findAll();
    List<Championship> findVisible();
    PageResult<Championship> findPage(int page, int size, boolean visibleOnly);
    void               delete(ChampionshipId id);
}
