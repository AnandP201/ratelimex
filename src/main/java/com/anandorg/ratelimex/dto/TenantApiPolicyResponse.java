package com.anandorg.ratelimex.dto;

import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.TenantApiPolicy;

public record TenantApiPolicyResponse(
        String tenantId,
        String api,
        boolean enabled,
        FailureMode failureMode,
        RateLimitConfigRequest tenantLimit,
        RateLimitConfigRequest apiLimit,
        RateLimitConfigRequest userLimit,
        long version
) {
    public static TenantApiPolicyResponse from(TenantApiPolicy policy) {
        return new TenantApiPolicyResponse(
                policy.tenantId(),
                policy.api(),
                policy.enabled(),
                policy.failureMode(),
                RateLimitConfigRequest.from(policy.tenantLimit()),
                RateLimitConfigRequest.from(policy.apiLimit()),
                RateLimitConfigRequest.from(policy.userLimit()),
                policy.version()
        );
    }
}
