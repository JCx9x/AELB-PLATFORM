package es.aelb.backendweb.domain.registration;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.domain.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository {

    void                   save(Registration registration);
    Optional<Registration> findById(String id);
    boolean                existsByUserAndChampionship(UserId userId, ChampionshipId championshipId);
    List<Registration>     findByUserAndChampionship(UserId userId, ChampionshipId championshipId);
    List<Registration>     findByChampionship(ChampionshipId championshipId);
    List<Registration>     findByUser(UserId userId);
    PageResult<Registration> findPageByChampionship(ChampionshipId championshipId, int page, int size);
    PageResult<Registration> findPageByUser(UserId userId, int page, int size);
    void                   delete(String id);
}
