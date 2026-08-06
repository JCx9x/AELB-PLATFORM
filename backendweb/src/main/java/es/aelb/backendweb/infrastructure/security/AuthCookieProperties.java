package es.aelb.backendweb.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the browser-only JWT cookies.
 * The access cookie is sent on every /api request and holds a short-lived
 * token. The refresh cookie is scoped to /api/auth only and holds the
 * opaque, rotating session used to mint new access tokens.
 */
@ConfigurationProperties(prefix = "aelb.auth.cookie")
public class AuthCookieProperties {

    private String name = "aelb_access";
    private String refreshName = "aelb_refresh";
    private boolean secure;
    private String sameSite = "Strict";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRefreshName() { return refreshName; }
    public void setRefreshName(String refreshName) { this.refreshName = refreshName; }

    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }

    public String getSameSite() { return sameSite; }
    public void setSameSite(String sameSite) { this.sameSite = sameSite; }
}
