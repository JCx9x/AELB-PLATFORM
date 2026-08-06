package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.valueobject.ResultId;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.ResultJpaEntity;

public class ResultJpaMapper {

    private ResultJpaMapper() {}

    public static ResultJpaEntity toJpa(Result result) {
        ResultJpaEntity e = new ResultJpaEntity();
        e.setId(result.getId().value());
        e.setTitle(result.getTitle());
        e.setDescription(result.getDescription());
        e.setPdfData(result.getPdfData());
        e.setPdfOriginalSize(result.getPdfOriginalSize());
        e.setPdfFileName(result.getPdfFileName());
        e.setPublished(result.isPublished());
        e.setPublishedAt(result.getPublishedAt());
        e.setAuthorId(result.getAuthorId().value());
        e.setCreatedAt(result.getCreatedAt());
        e.setUpdatedAt(result.getUpdatedAt());
        return e;
    }

    public static Result toDomain(ResultJpaEntity e) {
        return Result.reconstitute(
                ResultId.of(e.getId()),
                e.getTitle(),
                e.getDescription(),
                e.getPdfData(),
                e.getPdfOriginalSize(),
                e.getPdfFileName(),
                e.isPublished(),
                e.getPublishedAt(),
                UserId.of(e.getAuthorId()),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
