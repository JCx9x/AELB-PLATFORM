package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.shared.pagination.PageResult;

import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public static <S, T> PagedResponse<T> from(PageResult<S> page, Function<S, T> mapper) {
        return new PagedResponse<>(page.content().stream().map(mapper).toList(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
