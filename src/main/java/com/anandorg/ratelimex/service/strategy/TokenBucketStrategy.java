package com.anandorg.ratelimex.service.strategy;

import java.util.List;

import com.anandorg.ratelimex.config.RateLimiterConfigManager;
import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.repository.RateLimitStore;
import com.anandorg.ratelimex.repository.RateLimitStoreResult;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketStrategy implements RateLimitStrategy {

    private final RateLimitStore rateLimitStore;
    private final RateLimiterConfigManager configManager;

    public TokenBucketStrategy(RateLimitStore rateLimitStore, RateLimiterConfigManager configManager) {
        this.rateLimitStore = rateLimitStore;
        this.configManager = configManager;
    }

    @Override
    public RateLimitDecision allow(String userId, String api, int cost) {

        List<RateLimitBucket> buckets = configManager.bucketsFor(userId, api);

        RateLimitStoreResult result = rateLimitStore.consume(buckets, cost);

        if (result.allowed()) {
            return RateLimitDecision.allowed(result.remainingTokens());
        }

        return RateLimitDecision.limited(result.remainingTokens(), result.retryAfterMillis());
    }
}
