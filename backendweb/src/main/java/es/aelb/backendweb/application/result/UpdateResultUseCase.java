package es.aelb.backendweb.application.result;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.result.valueobject.ResultId;

import java.util.Base64;

public class UpdateResultUseCase {

    private final ResultRepository resultRepository;

    public UpdateResultUseCase(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public Result execute(UpdateResultCommand cmd) {
        Result result = resultRepository.findById(ResultId.of(cmd.resultId()))
                .orElseThrow(() -> new Result.NotFoundException(cmd.resultId()));

        result.update(cmd.title(), cmd.description());

        if (cmd.pdfBase64() != null && !cmd.pdfBase64().isBlank()) {
            byte[] rawBytes      = Base64.getDecoder().decode(cmd.pdfBase64());
            long   originalSize  = rawBytes.length;
            String compressedB64 = CreateResultUseCase.compressAndEncode(rawBytes);
            result.replacePdf(compressedB64, originalSize, cmd.pdfFileName());
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
