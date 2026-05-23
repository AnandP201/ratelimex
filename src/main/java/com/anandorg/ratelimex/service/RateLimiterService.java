package com.anandorg.ratelimex.service;

import com.anandorg.ratelimex.metrics.RateLimitMetrics;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.service.policy.RateLimitPolicyException;
import com.anandorg.ratelimex.service.strategy.RateLimitStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimitStrategy rateLimitStrategy;
    private final RateLimitMetrics metrics;

    public RateLimiterService(RateLimitStrategy rateLimitStrategy, RateLimitMetrics metrics) {
        this.rateLimitStrategy = rateLimitStrategy;
        this.metrics = metrics;
    }

    // When 'cost' param isn't sent
    public RateLimitDecision allowRequest(String tenantId, String userId, String api) {
        return allowRequest(tenantId, userId, api, 1);
    }

    public RateLimitDecision allowRequest(String tenantId, String userId, String api, int cost) {
        try {

            RateLimitDecision decision = rateLimitStrategy.allow(tenantId, userId, api, cost);
            metrics.recordDecision(decision.allowed(), decision.degraded(), decision.reason());
            return decision;

        } catch (RateLimitPolicyException ex) {

            log.info("Rate limit policy rejected request: {}", ex.getMessage());

            RateLimitDecision decision = RateLimitDecision.rejected(ex.reason());
            metrics.recordDecision(decision.allowed(), decision.degraded(), decision.reason());
            return decision;

        }
    }
}
