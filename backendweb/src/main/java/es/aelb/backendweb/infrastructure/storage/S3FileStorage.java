package es.aelb.backendweb.infrastructure.storage;

import es.aelb.backendweb.infrastructure.config.StorageProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Service
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public S3FileStorage(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .serverSideEncryption("AES256")
                        .build(),
                RequestBody.fromBytes(content)
        );
    }

    @Override
    public InputStream get(String key) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
    }
}
