package com.anandorg.ratelimex.model;

public record RateLimitDecision(
        boolean allowed,
        String reason,
        long remainingTokens,
        long retryAfterMillis,
        boolean degraded,
        FailureMode failureMode
) {
    public static RateLimitDecision allowed(long remainingTokens) {
        return new RateLimitDecision(true, "allowed", remainingTokens, 0, false, null);
    }

    public static RateLimitDecision limited(long remainingTokens, long retryAfterMillis) {
        return new RateLimitDecision(false, "rate_limited", remainingTokens, retryAfterMillis, false, null);
    }

    public static RateLimitDecision rejected(String reason) {
        return new RateLimitDecision(false, reason, 0, 0, false, null);
    }

    public static RateLimitDecision degraded(FailureMode failureMode) {
        // If degraded
        // Allow only if FAIL_OPEN policy set
        
        // If FAIL_CLOSED , reject the request
        boolean allowed = failureMode == FailureMode.FAIL_OPEN;
        String reason = allowed ? "backend_unavailable_fail_open" : "backend_unavailable_fail_closed";
        return new RateLimitDecision(allowed, reason, 0, allowed ? 0 : 1000, true, failureMode);
    }
}
