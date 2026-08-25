package es.aelb.backendweb.domain.document;

import es.aelb.backendweb.domain.document.valueobject.DocumentId;
import es.aelb.backendweb.domain.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    void               save(Document document);
    Optional<Document> findById(DocumentId id);
    Optional<Document> findPublishedById(DocumentId id);
    List<Document>     findAll();
    List<Document>     findPublished();
    PageResult<Document> findPage(int page, int size, boolean publishedOnly);
    void               delete(DocumentId id);
}
