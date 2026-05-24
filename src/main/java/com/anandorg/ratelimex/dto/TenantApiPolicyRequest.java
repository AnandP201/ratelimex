package com.anandorg.ratelimex.dto;

import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.TenantApiPolicy;

public record TenantApiPolicyRequest(
        String api,
        Boolean enabled,
        FailureMode failureMode,
        RateLimitConfigRequest tenantLimit,
        RateLimitConfigRequest apiLimit,
        RateLimitConfigRequest userLimit
) {
    public TenantApiPolicy toPolicy(String tenantId) {
        if (api == null || api.isBlank()) {
            throw new IllegalArgumentException("api is required");
        }
        if (tenantLimit == null) {
            throw new IllegalArgumentException("tenantLimit is required");
        }
        if (apiLimit == null) {
            throw new IllegalArgumentException("apiLimit is required");
        }
        if (userLimit == null) {
            throw new IllegalArgumentException("userLimit is required");
        }
        return new TenantApiPolicy(
                tenantId,
                api,
                enabled == null || enabled,
                failureMode == null ? FailureMode.FAIL_CLOSED : failureMode,
                tenantLimit.toConfig("tenantLimit"),
                apiLimit.toConfig("apiLimit"),
                userLimit.toConfig("userLimit"),
                0
        );
    }
}
