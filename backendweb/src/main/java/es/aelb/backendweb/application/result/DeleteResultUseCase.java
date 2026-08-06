package es.aelb.backendweb.application.result;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.result.valueobject.ResultId;

public class DeleteResultUseCase {

    private final ResultRepository resultRepository;

    public DeleteResultUseCase(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public void execute(String resultId) {
        resultRepository.findById(ResultId.of(resultId))
                .orElseThrow(() -> new Result.NotFoundException(resultId));
        resultRepository.delete(ResultId.of(resultId));
    }
}
