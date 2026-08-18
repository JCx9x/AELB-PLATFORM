package es.aelb.backendweb.infrastructure.storage;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * One-off migration for rows created before files moved to S3.
 * Activate explicitly with SPRING_PROFILES_ACTIVE=storage-migration.
 */
@Component
@Profile("storage-migration")
public class LegacyFileMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyFileMigrationRunner.class);

    private final DocumentRepository documents;
    private final ResultRepository results;
    private final FileStorage storage;

    public LegacyFileMigrationRunner(DocumentRepository documents, ResultRepository results, FileStorage storage) {
        this.documents = documents;
        this.results = results;
        this.storage = storage;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        int documentsMigrated = 0;
        for (Document document : documents.findAll()) {
            if (document.getFileStorageKey() != null || document.getFileData() == null) continue;
            byte[] content = decompress(document.getFileData());
            String key = StoredFileKeys.document(document.getId().value(), document.getFileName());
            storage.put(key, content, StoredFileKeys.contentType(document.getFileName()));
            document.attachFileStorageKey(key, document.getFileOriginalSize(), document.getFileName());
            documents.save(document);
            documentsMigrated++;
        }

        int resultsMigrated = 0;
        for (Result result : results.findAll()) {
            if (result.getPdfStorageKey() != null || result.getPdfData() == null) continue;
            byte[] content = decompress(result.getPdfData());
            String key = StoredFileKeys.result(result.getId().value(), result.getPdfFileName());
            storage.put(key, content, "application/pdf");
            result.attachPdfStorageKey(key, result.getPdfOriginalSize(), result.getPdfFileName());
            results.save(result);
            resultsMigrated++;
        }

        log.info("Legacy file migration finished: {} documents and {} results moved to S3", documentsMigrated, resultsMigrated);
    }

    private static byte[] decompress(String encoded) {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo migrar un fichero legacy a S3", e);
        }
    }
}
