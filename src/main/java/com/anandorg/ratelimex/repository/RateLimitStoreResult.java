package com.anandorg.ratelimex.repository;

public record RateLimitStoreResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterMillis
) {

}
