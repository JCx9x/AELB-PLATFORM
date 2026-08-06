package es.aelb.backendweb.domain.result;

import es.aelb.backendweb.domain.result.valueobject.ResultId;

import java.util.List;
import java.util.Optional;

public interface ResultRepository {

    void               save(Result result);
    Optional<Result>   findById(ResultId id);
    List<Result>       findAll();
    List<Result>       findPublished();
    void               delete(ResultId id);
}
