package es.aelb.backendweb.application.result;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.storage.FileStorage;
import es.aelb.backendweb.infrastructure.storage.StoredFileKeys;

import es.aelb.backendweb.infrastructure.storage.FileUploadValidator;

public class CreateResultUseCase {

    private final ResultRepository resultRepository;
    private final FileStorage fileStorage;

    public CreateResultUseCase(ResultRepository resultRepository, FileStorage fileStorage) {
        this.resultRepository = resultRepository;
        this.fileStorage = fileStorage;
    }

    public Result execute(CreateResultCommand cmd) {
        byte[] rawBytes       = FileUploadValidator.decodePdf(cmd.pdfBase64(), cmd.pdfFileName());
        long   originalSize   = rawBytes.length;
        Result result = Result.create(
                cmd.title(), cmd.description(),
                null, originalSize, cmd.pdfFileName(),
                UserId.of(cmd.authorUserId())
        );

        String storageKey = StoredFileKeys.result(result.getId().value(), cmd.pdfFileName());
        fileStorage.put(storageKey, rawBytes, "application/pdf");
        result.attachPdfStorageKey(storageKey, originalSize, cmd.pdfFileName());

        if (cmd.published()) result.publish();

        resultRepository.save(result);
        return result;
    }

}
