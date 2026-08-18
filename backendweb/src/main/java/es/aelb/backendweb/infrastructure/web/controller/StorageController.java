package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.infrastructure.storage.PresignedUrlService;
import es.aelb.backendweb.infrastructure.web.dto.response.PresignedUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Set;

@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private static final Set<String> ALLOWED_FOLDERS = Set.of("news", "championships");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final PresignedUrlService presignedUrlService;

    public StorageController(PresignedUrlService presignedUrlService) {
        this.presignedUrlService = presignedUrlService;
    }

    /**
     * Returns a presigned PUT URL the browser uses to upload directly to MinIO/S3.
     * @param folder      e.g. "news" or "championships" — used as key prefix
     * @param contentType e.g. "image/jpeg"
     */
    @PostMapping("/presign/upload")
    public ResponseEntity<PresignedUploadResponse> presignUpload(
            @RequestParam String folder,
            @RequestParam String contentType,
            @RequestParam long contentLength
    ) {
        if (!ALLOWED_FOLDERS.contains(folder) || !ALLOWED_IMAGE_TYPES.contains(contentType)
                || contentLength <= 0 || contentLength > presignedUrlService.maxUploadBytes()) {
            return ResponseEntity.badRequest().build();
        }
        String ext = switch (contentType) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default           -> throw new IllegalArgumentException("Tipo de imagen no permitido");
        };
        String key       = folder + "/" + UUID.randomUUID() + ext;
        String uploadUrl = presignedUrlService.generateUploadUrl(key, contentType, contentLength);
        return ResponseEntity.ok(new PresignedUploadResponse(uploadUrl, key));
    }
}
