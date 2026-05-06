package com.anandorg.ratelimex.config;

import java.util.HashMap;
import java.util.Map;

import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.RateLimitConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ratelimex")
public class RatelimexProperties {

    private String namespace = "default";
    private FailureMode failureMode = FailureMode.FAIL_CLOSED;
    private RateLimitConfig globalLimit = new RateLimitConfig(10_000, 1_000.0, 120);
    private RateLimitConfig defaultApiLimit = new RateLimitConfig(1_000, 100.0, 120);
    private RateLimitConfig defaultUserLimit = new RateLimitConfig(60, 10.0, 120);

    private Map<String, RateLimitConfig> apiLimits = new HashMap<>();
    private Map<String, RateLimitConfig> userLimits = new HashMap<>();

    public String getNamespace() {
        return namespace;
    }
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public FailureMode getFailureMode() {
        return failureMode;
    }
    public void setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
    }

    public RateLimitConfig getGlobalLimit() {
        return globalLimit;
    }
    public void setGlobalLimit(RateLimitConfig globalLimit) {
        this.globalLimit = globalLimit;
    }

    public RateLimitConfig getDefaultApiLimit() {
        return defaultApiLimit;
    }
    public void setDefaultApiLimit(RateLimitConfig defaultApiLimit) {
        this.defaultApiLimit = defaultApiLimit;
    }

    public RateLimitConfig getDefaultUserLimit() {
        return defaultUserLimit;
    }
    public void setDefaultUserLimit(RateLimitConfig defaultUserLimit) {
        this.defaultUserLimit = defaultUserLimit;
    }

    public Map<String, RateLimitConfig> getApiLimits() {
        return apiLimits;
    }
    public void setApiLimits(Map<String, RateLimitConfig> apiLimits) {
        this.apiLimits = apiLimits;
    }

    public Map<String, RateLimitConfig> getUserLimits() {
        return userLimits;
    };
    public void setUserLimits(Map<String, RateLimitConfig> userLimits) {
        this.userLimits = userLimits;
    }
}
