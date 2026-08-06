package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.news.News;

import java.time.LocalDateTime;

public record NewsResponse(
        String        id,
        String        title,
        String        slug,
        String        content,
        boolean       hasImage,
        String        imageType,
        String        imageUrl,    // presigned GET URL (S3 key) or null; legacy images use /api/news/{id}/image
        boolean       published,
        LocalDateTime publishedAt,
        String        authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NewsResponse from(News news, String resolvedImageUrl) {
        return new NewsResponse(
                news.getId().value(),
                news.getTitle(),
                news.getSlug(),
                news.getContent(),
                news.hasImage(),
                news.getImageType(),
                resolvedImageUrl,
                news.isPublished(),
                news.getPublishedAt(),
                news.getAuthorId().value(),
                news.getCreatedAt(),
                news.getUpdatedAt()
        );
    }
}
