package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.document.CreateDocumentCommand;
import es.aelb.backendweb.application.document.CreateDocumentUseCase;
import es.aelb.backendweb.application.document.DeleteDocumentUseCase;
import es.aelb.backendweb.application.document.UpdateDocumentCommand;
import es.aelb.backendweb.application.document.UpdateDocumentUseCase;
import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.document.valueobject.DocumentId;
import es.aelb.backendweb.infrastructure.web.dto.request.CreateDocumentRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdateDocumentRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.DocumentResponse;
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
@RequestMapping("/api/documents")
public class DocumentController {

    private final CreateDocumentUseCase createDocumentUseCase;
    private final UpdateDocumentUseCase updateDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final DocumentRepository    documentRepository;

    public DocumentController(
            CreateDocumentUseCase createDocumentUseCase,
            UpdateDocumentUseCase updateDocumentUseCase,
            DeleteDocumentUseCase deleteDocumentUseCase,
            DocumentRepository documentRepository
    ) {
        this.createDocumentUseCase = createDocumentUseCase;
        this.updateDocumentUseCase = updateDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.documentRepository    = documentRepository;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> findPublished() {
        return ResponseEntity.ok(documentRepository.findPublished().stream()
                .map(DocumentResponse::from).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<DocumentResponse>> findAll() {
        return ResponseEntity.ok(documentRepository.findAll().stream()
                .map(DocumentResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> findById(@PathVariable String id) {
        return documentRepository.findById(DocumentId.of(id))
                .map(DocumentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String id) {
        Document document = documentRepository.findById(DocumentId.of(id)).orElse(null);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] compressedBytes = Base64.getDecoder().decode(document.getFileData());
        byte[] fileBytes       = decompress(compressedBytes);
        String fileName        = sanitizeFileName(document.getFileName());

        MediaType mediaType = detectMediaType(fileName);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(fileBytes);
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @Valid @RequestBody CreateDocumentRequest req,
            Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        Document created = createDocumentUseCase.execute(new CreateDocumentCommand(
                req.title(), req.description(),
                req.fileBase64(), req.fileName(),
                req.published(), userId
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId().value()).toUri();
        return ResponseEntity.created(location).body(DocumentResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateDocumentRequest req
    ) {
        Document updated = updateDocumentUseCase.execute(new UpdateDocumentCommand(
                id, req.title(), req.description(),
                req.fileBase64(), req.fileName(),
                req.published()
        ));
        return ResponseEntity.ok(DocumentResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteDocumentUseCase.execute(id);
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
            throw new IllegalStateException("Error al descomprimir el documento", e);
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "documento.pdf";
        return name.replaceAll("[^\\w.\\-]", "_");
    }

    private static MediaType detectMediaType(String fileName) {
        if (fileName == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf"))  return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".docx")) return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (lower.endsWith(".xlsx")) return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
