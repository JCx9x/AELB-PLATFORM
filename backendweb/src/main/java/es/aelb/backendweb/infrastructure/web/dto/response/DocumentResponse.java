package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.document.Document;

import java.time.LocalDateTime;

public record DocumentResponse(
        String        id,
        String        title,
        String        description,
        long          fileOriginalSize,
        String        fileName,
        boolean       published,
        LocalDateTime publishedAt,
        String        authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId().value(),
                d.getTitle(),
                d.getDescription(),
                d.getFileOriginalSize(),
                d.getFileName(),
                d.isPublished(),
                d.getPublishedAt(),
                d.getAuthorId().value(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
