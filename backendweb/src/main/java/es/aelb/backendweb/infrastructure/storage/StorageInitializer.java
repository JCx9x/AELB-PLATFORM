package es.aelb.backendweb.infrastructure.storage;

import es.aelb.backendweb.infrastructure.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
public class StorageInitializer {

    private final S3Client          s3Client;
    private final StorageProperties props;

    public StorageInitializer(S3Client s3Client, StorageProperties props) {
        this.s3Client = s3Client;
        this.props    = props;
    }

    @PostConstruct
    void init() {
        try {
            ensureBucketExists();
        } catch (Exception e) {
            System.err.println("[StorageInitializer] Could not ensure bucket exists: " + e.getMessage());
            return;
        }
        try {
            configureCors();
        } catch (Exception e) {
            System.err.println("[StorageInitializer] Could not configure bucket CORS: " + e.getMessage());
        }
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(r -> r.bucket(props.getBucket()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || e.statusCode() == 301) {
                s3Client.createBucket(r -> r.bucket(props.getBucket()));
            } else {
                throw e;
            }
        }
    }

    private void configureCors() {
        CORSRule rule = CORSRule.builder()
                .allowedOrigins("*")
                .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
                .allowedHeaders("*")
                .exposeHeaders("ETag")
                .maxAgeSeconds(3600)
                .build();

        s3Client.putBucketCors(r -> r
                .bucket(props.getBucket())
                .corsConfiguration(CORSConfiguration.builder()
                        .corsRules(rule)
                        .build()));
    }
}
