package es.aelb.backendweb.domain.document;

import es.aelb.backendweb.domain.document.valueobject.DocumentId;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    void               save(Document document);
    Optional<Document> findById(DocumentId id);
    List<Document>     findAll();
    List<Document>     findPublished();
    void               delete(DocumentId id);
}
