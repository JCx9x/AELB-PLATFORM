package es.aelb.backendweb.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, StripeProperties.class, EmailProperties.class})
public class S3Config {

    @Bean
    public S3Client s3Client(StorageProperties props) {
        var builder = S3Client.builder()
                .region(Region.of(props.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());
        configure(builder, props, props.getEndpointOverride());
        return builder.build();
    }

    /**
     * Presigner uses the PUBLIC endpoint so generated URLs are browser-accessible.
     * Internal operations (bucket management) use S3Client with the internal endpoint.
     */
    @Bean
    public S3Presigner s3Presigner(StorageProperties props) {
        var builder = S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());
        configure(builder, props, props.getPublicEndpoint());
        return builder.build();
    }

    private void configure(S3ClientBuilder builder, StorageProperties props, String endpoint) {
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        builder.credentialsProvider(props.isUseIamRole()
                ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())));
    }

    private void configure(S3Presigner.Builder builder, StorageProperties props, String endpoint) {
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        builder.credentialsProvider(props.isUseIamRole()
                ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())));
    }
}
