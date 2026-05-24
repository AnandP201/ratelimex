package com.anandorg.ratelimex.service.policy;

public class RateLimitPolicyException extends RuntimeException {

    private final String reason;

    public RateLimitPolicyException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
