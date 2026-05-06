package com.anandorg.ratelimex.service;

import com.anandorg.ratelimex.config.RatelimexProperties;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.repository.RateLimitBackendException;
import com.anandorg.ratelimex.service.strategy.RateLimitStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimitStrategy rateLimitStrategy;
    private final RatelimexProperties properties;

    public RateLimiterService(RateLimitStrategy rateLimitStrategy, RatelimexProperties properties) {
        this.rateLimitStrategy = rateLimitStrategy;
        this.properties = properties;
    }

    // When 'cost' param isn't sent
    public RateLimitDecision allowRequest(String userId, String api) {
        return allowRequest(userId, api, 1);
    }

    public RateLimitDecision allowRequest(String userId, String api, int cost) {
        try {

            return rateLimitStrategy.allow(userId, api, cost);

        } catch (RateLimitBackendException ex) {

            log.warn("Rate limiter backend failed; applying {}: {}", properties.getFailureMode(), ex.getMessage());

            return RateLimitDecision.degraded(properties.getFailureMode());

        }
    }
}
