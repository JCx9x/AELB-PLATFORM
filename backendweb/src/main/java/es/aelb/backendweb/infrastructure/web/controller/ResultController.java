package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.result.CreateResultCommand;
import es.aelb.backendweb.application.result.CreateResultUseCase;
import es.aelb.backendweb.application.result.DeleteResultUseCase;
import es.aelb.backendweb.application.result.UpdateResultCommand;
import es.aelb.backendweb.application.result.UpdateResultUseCase;
import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.result.valueobject.ResultId;
import es.aelb.backendweb.infrastructure.web.dto.request.CreateResultRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdateResultRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.ResultResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final CreateResultUseCase createResultUseCase;
    private final UpdateResultUseCase updateResultUseCase;
    private final DeleteResultUseCase deleteResultUseCase;
    private final ResultRepository    resultRepository;

    public ResultController(
            CreateResultUseCase createResultUseCase,
            UpdateResultUseCase updateResultUseCase,
            DeleteResultUseCase deleteResultUseCase,
            ResultRepository resultRepository
    ) {
        this.createResultUseCase = createResultUseCase;
        this.updateResultUseCase = updateResultUseCase;
        this.deleteResultUseCase = deleteResultUseCase;
        this.resultRepository    = resultRepository;
    }

    @GetMapping
    public ResponseEntity<List<ResultResponse>> findPublished() {
        return ResponseEntity.ok(resultRepository.findPublished().stream()
                .map(ResultResponse::from).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ResultResponse>> findAll() {
        return ResponseEntity.ok(resultRepository.findAll().stream()
                .map(ResultResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResultResponse> findById(@PathVariable String id) {
        return resultRepository.findById(ResultId.of(id))
                .map(ResultResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        Result result = resultRepository.findById(ResultId.of(id)).orElse(null);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] compressedBytes = Base64.getDecoder().decode(result.getPdfData());
        byte[] pdfBytes        = decompress(compressedBytes);

        String fileName = sanitizeFileName(result.getPdfFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdfBytes);
    }

    @PostMapping
    public ResponseEntity<ResultResponse> create(
            @Valid @RequestBody CreateResultRequest req,
            Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        Result created = createResultUseCase.execute(new CreateResultCommand(
                req.title(), req.description(),
                req.pdfBase64(), req.pdfFileName(),
                req.published(), userId
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId().value()).toUri();
        return ResponseEntity.created(location).body(ResultResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResultResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateResultRequest req
    ) {
        Result updated = updateResultUseCase.execute(new UpdateResultCommand(
                id, req.title(), req.description(),
                req.pdfBase64(), req.pdfFileName(),
                req.published()
        ));
        return ResponseEntity.ok(ResultResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteResultUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    private static byte[] decompress(byte[] compressed) {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al descomprimir el PDF", e);
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "resultado.pdf";
        String safe = name.replaceAll("[^\\w.\\-]", "_");
        return safe.endsWith(".pdf") ? safe : safe + ".pdf";
    }
}
