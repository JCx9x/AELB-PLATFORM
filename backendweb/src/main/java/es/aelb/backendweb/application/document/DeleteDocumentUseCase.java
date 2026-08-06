package es.aelb.backendweb.application.document;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.document.valueobject.DocumentId;

public class DeleteDocumentUseCase {

    private final DocumentRepository documentRepository;

    public DeleteDocumentUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void execute(String documentId) {
        documentRepository.findById(DocumentId.of(documentId))
                .orElseThrow(() -> new Document.NotFoundException(documentId));
        documentRepository.delete(DocumentId.of(documentId));
    }
}
