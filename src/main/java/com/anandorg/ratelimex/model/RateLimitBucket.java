package com.anandorg.ratelimex.model;

public record RateLimitBucket(
        LimitScope scope,
        String key,
        RateLimitConfig config
) {
}
