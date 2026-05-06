package com.anandorg.ratelimex.dto;

import com.anandorg.ratelimex.model.RateLimitDecision;

public record RateLimitCheckResponse(
        boolean allowed,
        String reason,
        long remainingTokens,
        long retryAfterMillis,
        boolean degraded
) {
    public static RateLimitCheckResponse from(RateLimitDecision decision) {
        return new RateLimitCheckResponse(
                decision.allowed(),
                decision.reason(),
                decision.remainingTokens(),
                decision.retryAfterMillis(),
                decision.degraded()
        );
    }
}
