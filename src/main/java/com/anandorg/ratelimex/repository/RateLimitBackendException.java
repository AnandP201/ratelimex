package com.anandorg.ratelimex.repository;

public class RateLimitBackendException extends RuntimeException {

    public RateLimitBackendException(String message) {
        super(message);
    }

    public RateLimitBackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
