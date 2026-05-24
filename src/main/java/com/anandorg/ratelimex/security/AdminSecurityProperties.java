package com.anandorg.ratelimex.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ratelimex.security")
public class AdminSecurityProperties {

    private String adminApiKey;
    private String adminApiKeyHeader = "X-Admin-Api-Key";

    public String getAdminApiKey() {
        return adminApiKey;
    }

    public void setAdminApiKey(String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    public String getAdminApiKeyHeader() {
        return adminApiKeyHeader;
    }

    public void setAdminApiKeyHeader(String adminApiKeyHeader) {
        this.adminApiKeyHeader = adminApiKeyHeader;
    }

    public boolean hasConfiguredAdminKey() {
        return adminApiKey != null && !adminApiKey.isBlank();
    }
}
