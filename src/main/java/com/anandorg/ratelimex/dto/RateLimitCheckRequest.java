package com.anandorg.ratelimex.dto;

public record RateLimitCheckRequest(
        String userId,
        String api,
        Integer cost
) {
    public int normalizedCost() {
        return cost == null ? 1 : cost;
    }
}
