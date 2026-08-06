package es.aelb.backendweb.application.document;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.document.valueobject.DocumentId;

import java.util.Base64;

public class UpdateDocumentUseCase {

    private final DocumentRepository documentRepository;

    public UpdateDocumentUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document execute(UpdateDocumentCommand cmd) {
        Document document = documentRepository.findById(DocumentId.of(cmd.documentId()))
                .orElseThrow(() -> new Document.NotFoundException(cmd.documentId()));

        document.update(cmd.title(), cmd.description());

        if (cmd.fileBase64() != null && !cmd.fileBase64().isBlank()) {
            byte[] rawBytes      = Base64.getDecoder().decode(cmd.fileBase64());
            long   originalSize  = rawBytes.length;
            String compressedB64 = CreateDocumentUseCase.compressAndEncode(rawBytes);
            document.replaceFile(compressedB64, originalSize, cmd.fileName());
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
