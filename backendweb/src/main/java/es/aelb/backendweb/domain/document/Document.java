package es.aelb.backendweb.domain.document;

import es.aelb.backendweb.domain.document.valueobject.DocumentId;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.LocalDateTime;

public class Document extends AggregateRoot<DocumentId> {

    private String        title;
    private String        description;
    private String        fileData;         // base64-encoded GZip-compressed file
    private long          fileOriginalSize; // original size in bytes before compression
    private String        fileName;         // original file name
    private boolean       published;
    private LocalDateTime publishedAt;
    private final UserId  authorId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Document(DocumentId id, String title, String description,
                     String fileData, long fileOriginalSize, String fileName,
                     UserId authorId, LocalDateTime createdAt) {
        super(id);
        this.title            = title;
        this.description      = description;
        this.fileData         = fileData;
        this.fileOriginalSize = fileOriginalSize;
        this.fileName         = fileName;
        this.authorId         = authorId;
        this.published        = false;
        this.createdAt        = createdAt;
        this.updatedAt        = createdAt;
    }

    public static Document create(String title, String description,
                                  String fileData, long fileOriginalSize, String fileName,
                                  UserId authorId) {
        validateTitle(title);
        return new Document(DocumentId.generate(), title, description,
                fileData, fileOriginalSize, fileName, authorId, LocalDateTime.now());
    }

    public static Document reconstitute(
            DocumentId id, String title, String description,
            String fileData, long fileOriginalSize, String fileName,
            boolean published, LocalDateTime publishedAt,
            UserId authorId, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        Document d = new Document(id, title, description, fileData, fileOriginalSize, fileName, authorId, createdAt);
        d.published   = published;
        d.publishedAt = publishedAt;
        d.updatedAt   = updatedAt;
        return d;
    }

    public void update(String title, String description) {
        validateTitle(title);
        this.title       = title;
        this.description = description;
        this.updatedAt   = LocalDateTime.now();
    }

    public void replaceFile(String fileData, long fileOriginalSize, String fileName) {
        this.fileData         = fileData;
        this.fileOriginalSize = fileOriginalSize;
        this.fileName         = fileName;
        this.updatedAt        = LocalDateTime.now();
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

    public String        getTitle()            { return title; }
    public String        getDescription()      { return description; }
    public String        getFileData()         { return fileData; }
    public long          getFileOriginalSize() { return fileOriginalSize; }
    public String        getFileName()         { return fileName; }
    public boolean       isPublished()         { return published; }
    public LocalDateTime getPublishedAt()      { return publishedAt; }
    public UserId        getAuthorId()         { return authorId; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Documento no encontrado: " + id);
        }
    }
}
