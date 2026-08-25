package es.aelb.backendweb.domain.result;

import es.aelb.backendweb.domain.result.valueobject.ResultId;
import es.aelb.backendweb.domain.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface ResultRepository {

    void               save(Result result);
    Optional<Result>   findById(ResultId id);
    Optional<Result>   findPublishedById(ResultId id);
    List<Result>       findAll();
    List<Result>       findPublished();
    PageResult<Result> findPage(int page, int size, boolean publishedOnly);
    void               delete(ResultId id);
}
