package com.anandorg.ratelimex.service.strategy;

import com.anandorg.ratelimex.config.RateLimiterConfigManager;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.model.ResolvedRateLimitPolicy;
import com.anandorg.ratelimex.repository.RateLimitBackendException;
import com.anandorg.ratelimex.repository.RateLimitStore;
import com.anandorg.ratelimex.repository.RateLimitStoreResult;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class TokenBucketStrategy implements RateLimitStrategy {

    private final RateLimitStore rateLimitStore;
    private final RateLimiterConfigManager configManager;

    public TokenBucketStrategy(RateLimitStore rateLimitStore, RateLimiterConfigManager configManager) {
        this.rateLimitStore = rateLimitStore;
        this.configManager = configManager;
    }

    @Override
    public RateLimitDecision allow(String tenantId, String userId, String api, int cost) {

        ResolvedRateLimitPolicy policy = configManager.resolve(tenantId, userId, api);

        try {
            RateLimitStoreResult result = rateLimitStore.consume(policy.buckets(), cost);

            if (result.allowed()) {
                return RateLimitDecision.allowed(result.remainingTokens());
            }
            return RateLimitDecision.limited(result.remainingTokens(), result.retryAfterMillis());
        } catch (RateLimitBackendException ex) {
            return RateLimitDecision.degraded(policy.failureMode());
        }
    }
}
