package com.anandorg.ratelimex.dto;

import com.anandorg.ratelimex.model.RateLimitConfig;

public record RateLimitConfigRequest(
        Integer capacity,
        Double refillTokensPerSecond,
        Integer ttlSeconds
) {
    public RateLimitConfig toConfig(String name) {
        if (capacity == null) {
            throw new IllegalArgumentException(name + ".capacity is required");
        }
        if (refillTokensPerSecond == null) {
            throw new IllegalArgumentException(name + ".refillTokensPerSecond is required");
        }
        if (ttlSeconds == null) {
            throw new IllegalArgumentException(name + ".ttlSeconds is required");
        }
        return new RateLimitConfig(capacity, refillTokensPerSecond, ttlSeconds);
    }

    public static RateLimitConfigRequest from(RateLimitConfig config) {
        return new RateLimitConfigRequest(
                config.getCapacity(),
                config.getRefillTokensPerSecond(),
                config.getTtlSeconds()
        );
    }
}
