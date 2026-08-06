package es.aelb.backendweb.infrastructure.storage;

import es.aelb.backendweb.infrastructure.config.StorageProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class PresignedUrlService {

    private final S3Presigner       presigner;
    private final StorageProperties props;

    public PresignedUrlService(S3Presigner presigner, StorageProperties props) {
        this.presigner = presigner;
        this.props     = props;
    }

    public String generateUploadUrl(String key, String contentType) {
        PutObjectPresignRequest req = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(props.getPresignUploadMinutes()))
                .putObjectRequest(r -> r
                        .bucket(props.getBucket())
                        .key(key)
                        .contentType(contentType))
                .build();
        return presigner.presignPutObject(req).url().toString();
    }

    public String generateReadUrl(String key) {
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(props.getPresignReadMinutes()))
                .getObjectRequest(r -> r
                        .bucket(props.getBucket())
                        .key(key))
                .build();
        return presigner.presignGetObject(req).url().toString();
    }

    /**
     * Resolves a stored image value to a displayable URL.
     * Legacy values that start with "http" are returned as-is.
     * S3 keys (short paths) are resolved to presigned GET URLs.
     */
    public String resolveUrl(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (stored.startsWith("http")) return stored;
        return generateReadUrl(stored);
    }
}
