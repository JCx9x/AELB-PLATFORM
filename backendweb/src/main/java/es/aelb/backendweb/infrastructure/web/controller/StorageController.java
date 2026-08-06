package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.infrastructure.storage.PresignedUrlService;
import es.aelb.backendweb.infrastructure.web.dto.response.PresignedUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
public class StorageController {

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
            @RequestParam String contentType
    ) {
        String ext = switch (contentType) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default           -> ".jpg";
        };
        String key       = folder + "/" + UUID.randomUUID() + ext;
        String uploadUrl = presignedUrlService.generateUploadUrl(key, contentType);
        return ResponseEntity.ok(new PresignedUploadResponse(uploadUrl, key));
    }
}
