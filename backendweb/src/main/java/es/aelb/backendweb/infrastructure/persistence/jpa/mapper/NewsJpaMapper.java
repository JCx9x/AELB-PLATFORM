package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.news.valueobject.NewsId;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.NewsJpaEntity;

public class NewsJpaMapper {

    private NewsJpaMapper() {}

    public static NewsJpaEntity toJpa(News news) {
        NewsJpaEntity e = new NewsJpaEntity();
        e.setId(news.getId().value());
        e.setTitle(news.getTitle());
        e.setSlug(news.getSlug());
        e.setContent(news.getContent());
        e.setImageData(news.getImageData());
        e.setImageType(news.getImageType());
        e.setImageKey(news.getImageKey());
        e.setPublished(news.isPublished());
        e.setPublishedAt(news.getPublishedAt());
        e.setAuthorId(news.getAuthorId().value());
        e.setCreatedAt(news.getCreatedAt());
        e.setUpdatedAt(news.getUpdatedAt());
        return e;
    }

    public static News toDomain(NewsJpaEntity e) {
        return News.reconstitute(
                NewsId.of(e.getId()),
                e.getTitle(),
                e.getSlug(),
                e.getContent(),
                e.getImageData(),
                e.getImageType(),
                e.getImageKey(),
                e.isPublished(),
                e.getPublishedAt(),
                UserId.of(e.getAuthorId()),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
