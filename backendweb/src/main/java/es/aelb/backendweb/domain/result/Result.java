package es.aelb.backendweb.domain.result;

import es.aelb.backendweb.domain.result.valueobject.ResultId;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.LocalDateTime;

public class Result extends AggregateRoot<ResultId> {

    private String        title;
    private String        description;
    private String        pdfData;         // base64-encoded GZip-compressed PDF
    private long          pdfOriginalSize; // original size in bytes before compression
    private String        pdfFileName;     // original file name
    private boolean       published;
    private LocalDateTime publishedAt;
    private final UserId  authorId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Result(ResultId id, String title, String description,
                   String pdfData, long pdfOriginalSize, String pdfFileName,
                   UserId authorId, LocalDateTime createdAt) {
        super(id);
        this.title           = title;
        this.description     = description;
        this.pdfData         = pdfData;
        this.pdfOriginalSize = pdfOriginalSize;
        this.pdfFileName     = pdfFileName;
        this.authorId        = authorId;
        this.published       = false;
        this.createdAt       = createdAt;
        this.updatedAt       = createdAt;
    }

    public static Result create(String title, String description,
                                String pdfData, long pdfOriginalSize, String pdfFileName,
                                UserId authorId) {
        validateTitle(title);
        return new Result(ResultId.generate(), title, description,
                pdfData, pdfOriginalSize, pdfFileName, authorId, LocalDateTime.now());
    }

    public static Result reconstitute(
            ResultId id, String title, String description,
            String pdfData, long pdfOriginalSize, String pdfFileName,
            boolean published, LocalDateTime publishedAt,
            UserId authorId, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        Result r = new Result(id, title, description, pdfData, pdfOriginalSize, pdfFileName, authorId, createdAt);
        r.published   = published;
        r.publishedAt = publishedAt;
        r.updatedAt   = updatedAt;
        return r;
    }

    public void update(String title, String description) {
        validateTitle(title);
        this.title       = title;
        this.description = description;
        this.updatedAt   = LocalDateTime.now();
    }

    public void replacePdf(String pdfData, long pdfOriginalSize, String pdfFileName) {
        this.pdfData         = pdfData;
        this.pdfOriginalSize = pdfOriginalSize;
        this.pdfFileName     = pdfFileName;
        this.updatedAt       = LocalDateTime.now();
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

    public String        getTitle()           { return title; }
    public String        getDescription()     { return description; }
    public String        getPdfData()         { return pdfData; }
    public long          getPdfOriginalSize() { return pdfOriginalSize; }
    public String        getPdfFileName()     { return pdfFileName; }
    public boolean       isPublished()        { return published; }
    public LocalDateTime getPublishedAt()     { return publishedAt; }
    public UserId        getAuthorId()        { return authorId; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Resultado no encontrado: " + id);
        }
    }
}
