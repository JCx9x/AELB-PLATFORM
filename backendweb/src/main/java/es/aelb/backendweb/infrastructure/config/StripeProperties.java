package es.aelb.backendweb.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aelb.stripe")
public class StripeProperties {
    private String secretKey = "";
    private String webhookSecret = "";
    private String currency = "eur";
    private String successUrl = "http://localhost:3000/perfil?stripe=success&session_id={CHECKOUT_SESSION_ID}";
    private String cancelUrl = "http://localhost:3000/perfil?stripe=cancelled";
    private int connectTimeoutMs = 3000;
    private int requestTimeoutMs = 10000;
    private int maxRetries = 2;
    private long retryBaseDelayMs = 200;
    private int circuitBreakerFailureThreshold = 5;
    private long circuitBreakerOpenMs = 30000;
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int value) { this.connectTimeoutMs = value; }
    public int getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(int value) { this.requestTimeoutMs = value; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int value) { this.maxRetries = value; }
    public long getRetryBaseDelayMs() { return retryBaseDelayMs; }
    public void setRetryBaseDelayMs(long value) { this.retryBaseDelayMs = value; }
    public int getCircuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
    public void setCircuitBreakerFailureThreshold(int value) { this.circuitBreakerFailureThreshold = value; }
    public long getCircuitBreakerOpenMs() { return circuitBreakerOpenMs; }
    public void setCircuitBreakerOpenMs(long value) { this.circuitBreakerOpenMs = value; }
}
