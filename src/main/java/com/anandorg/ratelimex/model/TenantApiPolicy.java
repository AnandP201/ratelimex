package com.anandorg.ratelimex.model;

public record TenantApiPolicy(
        String tenantId,
        String api,
        boolean enabled,
        FailureMode failureMode,
        RateLimitConfig tenantLimit,
        RateLimitConfig apiLimit,
        RateLimitConfig userLimit,
        long version
) {
    public void validate() {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("tenantId is required");
        }
        if (api == null || api.isBlank()) {
            throw new IllegalStateException("api is required");
        }
        if (failureMode == null) {
            throw new IllegalStateException("failureMode is required");
        }
        tenantLimit.validate("tenantLimit");
        apiLimit.validate("apiLimit");
        userLimit.validate("userLimit");
    }
}
