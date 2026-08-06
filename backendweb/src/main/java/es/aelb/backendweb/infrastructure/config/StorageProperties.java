package es.aelb.backendweb.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aelb.storage")
public class StorageProperties {

    private String bucket           = "aelb-images";
    private String region           = "us-east-1";
    private String accessKey        = "minioadmin";
    private String secretKey        = "minioadmin";
    private String endpointOverride = "http://localhost:9000";
    private String publicEndpoint   = "http://localhost:9000";
    private int    presignUploadMinutes = 15;
    private int    presignReadMinutes   = 1440;

    public String getBucket()               { return bucket; }
    public void   setBucket(String v)       { this.bucket = v; }

    public String getRegion()               { return region; }
    public void   setRegion(String v)       { this.region = v; }

    public String getAccessKey()            { return accessKey; }
    public void   setAccessKey(String v)    { this.accessKey = v; }

    public String getSecretKey()            { return secretKey; }
    public void   setSecretKey(String v)    { this.secretKey = v; }

    public String getEndpointOverride()             { return endpointOverride; }
    public void   setEndpointOverride(String v)     { this.endpointOverride = v; }

    public String getPublicEndpoint()               { return publicEndpoint; }
    public void   setPublicEndpoint(String v)       { this.publicEndpoint = v; }

    public int  getPresignUploadMinutes()            { return presignUploadMinutes; }
    public void setPresignUploadMinutes(int v)       { this.presignUploadMinutes = v; }

    public int  getPresignReadMinutes()              { return presignReadMinutes; }
    public void setPresignReadMinutes(int v)         { this.presignReadMinutes = v; }
}
