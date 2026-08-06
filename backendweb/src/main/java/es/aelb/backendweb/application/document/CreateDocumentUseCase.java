package es.aelb.backendweb.application.document;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

public class CreateDocumentUseCase {

    private final DocumentRepository documentRepository;

    public CreateDocumentUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document execute(CreateDocumentCommand cmd) {
        byte[] rawBytes      = Base64.getDecoder().decode(cmd.fileBase64());
        long   originalSize  = rawBytes.length;
        String compressedB64 = compressAndEncode(rawBytes);

        Document document = Document.create(
                cmd.title(), cmd.description(),
                compressedB64, originalSize, cmd.fileName(),
                UserId.of(cmd.authorUserId())
        );

        if (cmd.published()) document.publish();

        documentRepository.save(document);
        return document;
    }

    static String compressAndEncode(byte[] input) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(input);
            gzip.finish();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error al comprimir el documento", e);
        }
    }
}
