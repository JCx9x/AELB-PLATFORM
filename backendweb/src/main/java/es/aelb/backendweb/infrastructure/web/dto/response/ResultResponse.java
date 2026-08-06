package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.result.Result;

import java.time.LocalDateTime;

public record ResultResponse(
        String        id,
        String        title,
        String        description,
        long          pdfOriginalSize,
        String        pdfFileName,
        boolean       published,
        LocalDateTime publishedAt,
        String        authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResultResponse from(Result r) {
        return new ResultResponse(
                r.getId().value(),
                r.getTitle(),
                r.getDescription(),
                r.getPdfOriginalSize(),
                r.getPdfFileName(),
                r.isPublished(),
                r.getPublishedAt(),
                r.getAuthorId().value(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
