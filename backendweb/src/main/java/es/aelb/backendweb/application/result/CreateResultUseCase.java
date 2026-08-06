package es.aelb.backendweb.application.result;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

public class CreateResultUseCase {

    private final ResultRepository resultRepository;

    public CreateResultUseCase(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public Result execute(CreateResultCommand cmd) {
        byte[] rawBytes       = Base64.getDecoder().decode(cmd.pdfBase64());
        long   originalSize   = rawBytes.length;
        String compressedB64  = compressAndEncode(rawBytes);

        Result result = Result.create(
                cmd.title(), cmd.description(),
                compressedB64, originalSize, cmd.pdfFileName(),
                UserId.of(cmd.authorUserId())
        );

        if (cmd.published()) result.publish();

        resultRepository.save(result);
        return result;
    }

    static String compressAndEncode(byte[] input) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(input);
            gzip.finish();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error al comprimir el PDF", e);
        }
    }
}
