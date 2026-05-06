package com.anandorg.ratelimex.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.anandorg.ratelimex.config.RateLimiterConfigManager;
import com.anandorg.ratelimex.config.RatelimexProperties;
import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.repository.RateLimitStore;
import com.anandorg.ratelimex.repository.RateLimitStoreResult;
import org.junit.jupiter.api.Test;

class TokenBucketStrategyTest {

    @Test
    void returnsAllowedDecisionFromStoreResult() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(
                fixedStore(new RateLimitStoreResult(true, 42, 0)),
                new RateLimiterConfigManager(new RatelimexProperties())
        );

        RateLimitDecision decision = strategy.allow("u1", "/api/test", 1);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remainingTokens()).isEqualTo(42);
    }

    @Test
    void returnsLimitedDecisionFromStoreResult() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(
                fixedStore(new RateLimitStoreResult(false, 0, 250)),
                new RateLimiterConfigManager(new RatelimexProperties())
        );

        RateLimitDecision decision = strategy.allow("u1", "/api/test", 1);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterMillis()).isEqualTo(250);
    }

    private static RateLimitStore fixedStore(RateLimitStoreResult result) {
        return new RateLimitStore() {
            @Override
            public RateLimitStoreResult consume(List<RateLimitBucket> buckets, int cost) {
                return result;
            }
        };
    }
}
