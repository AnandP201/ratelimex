package com.anandorg.ratelimex.model;

import java.util.List;

public record ResolvedRateLimitPolicy(
        String tenantId,
        String api,
        FailureMode failureMode,
        List<RateLimitBucket> buckets
) {
}
