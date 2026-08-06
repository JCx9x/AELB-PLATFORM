package es.aelb.backendweb.domain.news;

import es.aelb.backendweb.domain.news.valueobject.NewsId;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.LocalDateTime;

public class News extends AggregateRoot<NewsId> {

    private String        title;
    private String        slug;
    private String        content;
    private String        imageData;  // legacy base64-encoded image, nullable
    private String        imageType;  // legacy MIME type, nullable
    private String        imageKey;   // S3/MinIO object key, nullable
    private boolean       published;
    private LocalDateTime publishedAt;
    private final UserId  authorId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private News(NewsId id, String title, String slug, String content,
                 UserId authorId, LocalDateTime createdAt) {
        super(id);
        this.title     = title;
        this.slug      = slug;
        this.content   = content;
        this.authorId  = authorId;
        this.published = false;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static News create(String title, String slug, String content, UserId authorId) {
        validateTitle(title);
        validateContent(content);
        return new News(NewsId.generate(), title, slug, content, authorId, LocalDateTime.now());
    }

    public static News reconstitute(
            NewsId id, String title, String slug, String content,
            String imageData, String imageType, String imageKey,
            boolean published, LocalDateTime publishedAt,
            UserId authorId, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        News n = new News(id, title, slug, content, authorId, createdAt);
        n.imageData   = imageData;
        n.imageType   = imageType;
        n.imageKey    = imageKey;
        n.published   = published;
        n.publishedAt = publishedAt;
        n.updatedAt   = updatedAt;
        return n;
    }

    public void update(String title, String content) {
        validateTitle(title);
        validateContent(content);
        this.title     = title;
        this.content   = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void setImageKey(String key) {
        this.imageKey  = key;
        this.imageData = null;
        this.imageType = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void setImage(String imageData, String imageType) {
        this.imageData = imageData;
        this.imageType = imageType;
        this.imageKey  = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void removeImage() {
        this.imageData = null;
        this.imageType = null;
        this.imageKey  = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        this.published   = true;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt   = LocalDateTime.now();
    }

    public void unpublish() {
        this.published = false;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        if (title.length() > 300) {
            throw new IllegalArgumentException("El título no puede superar los 300 caracteres");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El contenido de la noticia no puede estar vacío");
        }
    }

    public String        getTitle()       { return title; }
    public String        getSlug()        { return slug; }
    public String        getContent()     { return content; }
    public String        getImageData()   { return imageData; }
    public String        getImageType()   { return imageType; }
    public String        getImageKey()    { return imageKey; }
    public boolean       isPublished()    { return published; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public UserId        getAuthorId()    { return authorId; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getUpdatedAt()   { return updatedAt; }
    public boolean       hasImage()       { return (imageKey != null && !imageKey.isBlank())
                                                 || (imageData != null && !imageData.isBlank()); }
    public boolean       hasLegacyImage() { return imageData != null && !imageData.isBlank(); }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Noticia no encontrada: " + id);
        }
    }
}
