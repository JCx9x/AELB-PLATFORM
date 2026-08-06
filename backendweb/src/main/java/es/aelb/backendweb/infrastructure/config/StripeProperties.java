package es.aelb.backendweb.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aelb.stripe")
public class StripeProperties {
    private String secretKey = "";
    private String webhookSecret = "";
    private String currency = "eur";
    private String successUrl = "http://localhost:3000/perfil?stripe=success&session_id={CHECKOUT_SESSION_ID}";
    private String cancelUrl = "http://localhost:3000/perfil?stripe=cancelled";
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
}
