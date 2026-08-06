package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.valueobject.DocumentId;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.DocumentJpaEntity;

public class DocumentJpaMapper {

    private DocumentJpaMapper() {}

    public static DocumentJpaEntity toJpa(Document document) {
        DocumentJpaEntity e = new DocumentJpaEntity();
        e.setId(document.getId().value());
        e.setTitle(document.getTitle());
        e.setDescription(document.getDescription());
        e.setFileData(document.getFileData());
        e.setFileOriginalSize(document.getFileOriginalSize());
        e.setFileName(document.getFileName());
        e.setPublished(document.isPublished());
        e.setPublishedAt(document.getPublishedAt());
        e.setAuthorId(document.getAuthorId().value());
        e.setCreatedAt(document.getCreatedAt());
        e.setUpdatedAt(document.getUpdatedAt());
        return e;
    }

    public static Document toDomain(DocumentJpaEntity e) {
        return Document.reconstitute(
                DocumentId.of(e.getId()),
                e.getTitle(),
                e.getDescription(),
                e.getFileData(),
                e.getFileOriginalSize(),
                e.getFileName(),
                e.isPublished(),
                e.getPublishedAt(),
                UserId.of(e.getAuthorId()),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
