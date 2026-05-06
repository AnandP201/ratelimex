package com.anandorg.ratelimex.controller;

import com.anandorg.ratelimex.dto.RateLimitCheckRequest;
import com.anandorg.ratelimex.dto.RateLimitCheckResponse;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(@RequestHeader String userId) {
        RateLimitDecision decision = rateLimiterService.allowRequest(userId, "/api/test");
        HttpHeaders headers = headersFor(decision);
        if (!decision.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body("Rate Limit Exceeded");
        }
        return ResponseEntity.ok()
                .headers(headers)
                .body("Request allowed");
    }

    @PostMapping("/rate-limit/check")
    public ResponseEntity<RateLimitCheckResponse> check(@RequestBody RateLimitCheckRequest request) {

        validate(request);

        RateLimitDecision decision = rateLimiterService.allowRequest(
                request.userId(),
                request.api(),
                request.normalizedCost()
        );

        HttpStatus status = decision.allowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;

        return ResponseEntity.status(status)
                .headers(headersFor(decision))
                .body(RateLimitCheckResponse.from(decision));
    }

    private static void validate(RateLimitCheckRequest request) {

        if (request == null || request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        if (request.api() == null || request.api().isBlank()) {
            throw new IllegalArgumentException("api is required");
        }

        if (request.normalizedCost() <= 0) {
            throw new IllegalArgumentException("cost must be greater than zero");
        }
    }

    private static HttpHeaders headersFor(RateLimitDecision decision) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Remaining", Long.toString(decision.remainingTokens()));
        headers.add("X-RateLimit-Degraded", Boolean.toString(decision.degraded()));
        if (decision.retryAfterMillis() > 0) {
            headers.add(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, decision.retryAfterMillis() / 1000)));
        }
        return headers;
    }
}
