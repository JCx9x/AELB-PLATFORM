package es.aelb.backendweb.application.document;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.document.valueobject.DocumentId;
import es.aelb.backendweb.infrastructure.storage.FileStorage;
import es.aelb.backendweb.infrastructure.storage.StoredFileKeys;

import java.util.Base64;

public class UpdateDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;

    public UpdateDocumentUseCase(DocumentRepository documentRepository, FileStorage fileStorage) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
    }

    public Document execute(UpdateDocumentCommand cmd) {
        Document document = documentRepository.findById(DocumentId.of(cmd.documentId()))
                .orElseThrow(() -> new Document.NotFoundException(cmd.documentId()));

        document.update(cmd.title(), cmd.description());

        if (cmd.fileBase64() != null && !cmd.fileBase64().isBlank()) {
            byte[] rawBytes      = Base64.getDecoder().decode(cmd.fileBase64());
            long   originalSize  = rawBytes.length;
            String storageKey = StoredFileKeys.document(document.getId().value(), cmd.fileName());
            fileStorage.put(storageKey, rawBytes, StoredFileKeys.contentType(cmd.fileName()));
            if (document.getFileStorageKey() != null && !document.getFileStorageKey().equals(storageKey)) {
                fileStorage.delete(document.getFileStorageKey());
            }
            document.attachFileStorageKey(storageKey, originalSize, cmd.fileName());
        }

        if (cmd.published()) {
            document.publish();
        } else {
            document.unpublish();
        }

        documentRepository.save(document);
        return document;
    }
}
