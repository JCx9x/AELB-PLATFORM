package es.aelb.backendweb.application.document;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.storage.FileStorage;
import es.aelb.backendweb.infrastructure.storage.StoredFileKeys;

import java.util.Base64;

public class CreateDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;

    public CreateDocumentUseCase(DocumentRepository documentRepository, FileStorage fileStorage) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
    }

    public Document execute(CreateDocumentCommand cmd) {
        byte[] rawBytes      = Base64.getDecoder().decode(cmd.fileBase64());
        long   originalSize  = rawBytes.length;
        Document document = Document.create(
                cmd.title(), cmd.description(),
                null, originalSize, cmd.fileName(),
                UserId.of(cmd.authorUserId())
        );

        String storageKey = StoredFileKeys.document(document.getId().value(), cmd.fileName());
        fileStorage.put(storageKey, rawBytes, StoredFileKeys.contentType(cmd.fileName()));
        document.attachFileStorageKey(storageKey, originalSize, cmd.fileName());

        if (cmd.published()) document.publish();

        documentRepository.save(document);
        return document;
    }

}
