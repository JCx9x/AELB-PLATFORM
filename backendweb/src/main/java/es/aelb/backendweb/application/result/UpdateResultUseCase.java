package es.aelb.backendweb.application.result;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.result.valueobject.ResultId;
import es.aelb.backendweb.infrastructure.storage.FileStorage;
import es.aelb.backendweb.infrastructure.storage.StoredFileKeys;

import java.util.Base64;

public class UpdateResultUseCase {

    private final ResultRepository resultRepository;
    private final FileStorage fileStorage;

    public UpdateResultUseCase(ResultRepository resultRepository, FileStorage fileStorage) {
        this.resultRepository = resultRepository;
        this.fileStorage = fileStorage;
    }

    public Result execute(UpdateResultCommand cmd) {
        Result result = resultRepository.findById(ResultId.of(cmd.resultId()))
                .orElseThrow(() -> new Result.NotFoundException(cmd.resultId()));

        result.update(cmd.title(), cmd.description());

        if (cmd.pdfBase64() != null && !cmd.pdfBase64().isBlank()) {
            byte[] rawBytes      = Base64.getDecoder().decode(cmd.pdfBase64());
            long   originalSize  = rawBytes.length;
            String storageKey = StoredFileKeys.result(result.getId().value(), cmd.pdfFileName());
            fileStorage.put(storageKey, rawBytes, "application/pdf");
            if (result.getPdfStorageKey() != null && !result.getPdfStorageKey().equals(storageKey)) {
                fileStorage.delete(result.getPdfStorageKey());
            }
            result.attachPdfStorageKey(storageKey, originalSize, cmd.pdfFileName());
        }

        if (cmd.published()) {
            result.publish();
        } else {
            result.unpublish();
        }

        resultRepository.save(result);
        return result;
    }
}
