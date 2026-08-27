package es.aelb.backendweb.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aelb.email")
public class EmailProperties {
    private boolean enabled = true;
    private String provider = "smtp";
    private String appEnv = "local";
    private String from = "noreply@localhost.test";
    private String fromName = "AELB";
    private boolean workerEnabled = true;
    private int maxPerSecond = 10;
    private int workerConcurrency = 1;
    private int batchSize = 10;
    private int maxRetries = 5;
    private int processingTimeoutMinutes = 10;
    private int sentRetentionDays = 30;
    private String frontendPublicUrl = "http://localhost:3000";
    private String awsRegion = "eu-south-2";
    private String smtpHost = "localhost";
    private int smtpPort = 1025;
    private boolean smtpAuth;
    private boolean smtpTls;
    private int passwordResetTokenExpirationMinutes = 30;
    private int passwordResetCooldownSeconds = 60;
    private int passwordResetMaxPerHour = 5;
    private int emailVerificationTokenExpirationHours = 24;
    private int emailVerificationCooldownSeconds = 60;
    private int emailVerificationMaxPerHour = 5;
    private String tokenEncryptionKey = "";

    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public String getProvider() { return provider; } public void setProvider(String v) { provider = v; }
    public String getAppEnv() { return appEnv; } public void setAppEnv(String v) { appEnv = v; }
    public String getFrom() { return from; } public void setFrom(String v) { from = v; }
    public String getFromName() { return fromName; } public void setFromName(String v) { fromName = v; }
    public boolean isWorkerEnabled() { return workerEnabled; } public void setWorkerEnabled(boolean v) { workerEnabled = v; }
    public int getMaxPerSecond() { return maxPerSecond; } public void setMaxPerSecond(int v) { maxPerSecond = v; }
    public int getWorkerConcurrency() { return workerConcurrency; } public void setWorkerConcurrency(int v) { workerConcurrency = v; }
    public int getBatchSize() { return batchSize; } public void setBatchSize(int v) { batchSize = v; }
    public int getMaxRetries() { return maxRetries; } public void setMaxRetries(int v) { maxRetries = v; }
    public int getProcessingTimeoutMinutes() { return processingTimeoutMinutes; } public void setProcessingTimeoutMinutes(int v) { processingTimeoutMinutes = v; }
    public int getSentRetentionDays() { return sentRetentionDays; } public void setSentRetentionDays(int v) { sentRetentionDays = v; }
    public String getFrontendPublicUrl() { return frontendPublicUrl; } public void setFrontendPublicUrl(String v) { frontendPublicUrl = v; }
    public String getAwsRegion() { return awsRegion; } public void setAwsRegion(String v) { awsRegion = v; }
    public String getSmtpHost() { return smtpHost; } public void setSmtpHost(String v) { smtpHost = v; }
    public int getSmtpPort() { return smtpPort; } public void setSmtpPort(int v) { smtpPort = v; }
    public boolean isSmtpAuth() { return smtpAuth; } public void setSmtpAuth(boolean v) { smtpAuth = v; }
    public boolean isSmtpTls() { return smtpTls; } public void setSmtpTls(boolean v) { smtpTls = v; }
    public int getPasswordResetTokenExpirationMinutes() { return passwordResetTokenExpirationMinutes; } public void setPasswordResetTokenExpirationMinutes(int v) { passwordResetTokenExpirationMinutes = v; }
    public int getPasswordResetCooldownSeconds() { return passwordResetCooldownSeconds; } public void setPasswordResetCooldownSeconds(int v) { passwordResetCooldownSeconds = v; }
    public int getPasswordResetMaxPerHour() { return passwordResetMaxPerHour; } public void setPasswordResetMaxPerHour(int v) { passwordResetMaxPerHour = v; }
    public int getEmailVerificationTokenExpirationHours() { return emailVerificationTokenExpirationHours; } public void setEmailVerificationTokenExpirationHours(int v) { emailVerificationTokenExpirationHours = v; }
    public int getEmailVerificationCooldownSeconds() { return emailVerificationCooldownSeconds; } public void setEmailVerificationCooldownSeconds(int v) { emailVerificationCooldownSeconds = v; }
    public int getEmailVerificationMaxPerHour() { return emailVerificationMaxPerHour; } public void setEmailVerificationMaxPerHour(int v) { emailVerificationMaxPerHour = v; }
    public String getTokenEncryptionKey() { return tokenEncryptionKey; } public void setTokenEncryptionKey(String v) { tokenEncryptionKey = v; }
}
