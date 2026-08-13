package es.aelb.backendweb.application.document;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.document.valueobject.DocumentId;
import es.aelb.backendweb.infrastructure.storage.FileStorage;

public class DeleteDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;

    public DeleteDocumentUseCase(DocumentRepository documentRepository, FileStorage fileStorage) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
    }

    public void execute(String documentId) {
        Document document = documentRepository.findById(DocumentId.of(documentId))
                .orElseThrow(() -> new Document.NotFoundException(documentId));
        if (document.getFileStorageKey() != null) fileStorage.delete(document.getFileStorageKey());
        documentRepository.delete(DocumentId.of(documentId));
    }
}
