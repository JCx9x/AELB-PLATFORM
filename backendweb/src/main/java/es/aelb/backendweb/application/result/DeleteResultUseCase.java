package es.aelb.backendweb.application.result;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.result.valueobject.ResultId;
import es.aelb.backendweb.infrastructure.storage.FileStorage;

public class DeleteResultUseCase {

    private final ResultRepository resultRepository;
    private final FileStorage fileStorage;

    public DeleteResultUseCase(ResultRepository resultRepository, FileStorage fileStorage) {
        this.resultRepository = resultRepository;
        this.fileStorage = fileStorage;
    }

    public void execute(String resultId) {
        Result result = resultRepository.findById(ResultId.of(resultId))
                .orElseThrow(() -> new Result.NotFoundException(resultId));
        if (result.getPdfStorageKey() != null) fileStorage.delete(result.getPdfStorageKey());
        resultRepository.delete(ResultId.of(resultId));
    }
}
