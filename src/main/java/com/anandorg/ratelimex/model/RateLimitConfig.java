package com.anandorg.ratelimex.model;

public class RateLimitConfig {

    private int capacity;
    private double refillTokensPerSecond;
    private int ttlSeconds;

    public RateLimitConfig() {
        this(100, 10.0, 120);
    }

    public RateLimitConfig(int capacity, double refillTokensPerSecond) {
        this(capacity, refillTokensPerSecond, calculateTtlSeconds(capacity, refillTokensPerSecond));
    }

    public RateLimitConfig(int capacity, double refillTokensPerSecond, int ttlSeconds) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        if (refillTokensPerSecond < 0) {
            throw new IllegalArgumentException("refillTokensPerSecond cannot be negative");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be greater than zero");
        }
        this.capacity = capacity;
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.ttlSeconds = ttlSeconds;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getRefillTokensPerSecond() {
        return refillTokensPerSecond;
    }

    public void setRefillTokensPerSecond(double refillTokensPerSecond) {
        this.refillTokensPerSecond = refillTokensPerSecond;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public void validate(String name) {
        if (capacity <= 0) {
            throw new IllegalStateException(name + ".capacity must be greater than zero");
        }
        if (refillTokensPerSecond < 0) {
            throw new IllegalStateException(name + ".refillTokensPerSecond cannot be negative");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalStateException(name + ".ttlSeconds must be greater than zero");
        }
    }

    private static int calculateTtlSeconds(int capacity, double refillTokensPerSecond) {
        if (refillTokensPerSecond <= 0) {
            return 3600;
        }
        return Math.max(60, (int) Math.ceil((capacity / refillTokensPerSecond) * 2));
    }
}
