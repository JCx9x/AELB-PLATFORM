package es.aelb.backendweb.domain.quota;

import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.util.List;
import java.util.Optional;

public interface QuotaRepository {

    void            save(Quota quota);
    Optional<Quota> findByUserAndYear(UserId userId, int year);
    boolean         existsByUserAndYear(UserId userId, int year);
    List<Quota>     findByUser(UserId userId);
    List<String>     findPaidUserIdsByYear(int year);
}
