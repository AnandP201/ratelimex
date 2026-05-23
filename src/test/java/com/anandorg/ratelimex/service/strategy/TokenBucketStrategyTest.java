package com.anandorg.ratelimex.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.anandorg.ratelimex.config.RateLimiterConfigManager;
import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.LimitScope;
import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.RateLimitConfig;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.model.ResolvedRateLimitPolicy;
import com.anandorg.ratelimex.repository.RateLimitBackendException;
import com.anandorg.ratelimex.repository.RateLimitStore;
import com.anandorg.ratelimex.repository.RateLimitStoreResult;
import org.junit.jupiter.api.Test;

class TokenBucketStrategyTest {

    @Test
    void returnsAllowedDecisionFromStoreResult() {

        TokenBucketStrategy strategy = new TokenBucketStrategy(
                fixedStore(new RateLimitStoreResult(true, 42, 0)),
                configManager(FailureMode.FAIL_CLOSED)
        );

        RateLimitDecision decision = strategy.allow("tenant-a", "u1", "/api/test", 1);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remainingTokens()).isEqualTo(42);
    }

    @Test
    void returnsLimitedDecisionFromStoreResult() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(
                fixedStore(new RateLimitStoreResult(false, 0, 250)),
                configManager(FailureMode.FAIL_CLOSED)
        );

        RateLimitDecision decision = strategy.allow("tenant-a", "u1", "/api/test", 1);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterMillis()).isEqualTo(250);
    }

    @Test
    void appliesTenantApiFailureModeWhenRedisFails() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(failingStore(), configManager(FailureMode.FAIL_OPEN));

        RateLimitDecision decision = strategy.allow("tenant-a", "u1", "/api/test", 1);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.degraded()).isTrue();
        assertThat(decision.reason()).isEqualTo("backend_unavailable_fail_open");
    }

    private static RateLimiterConfigManager configManager(FailureMode failureMode) {
        RateLimiterConfigManager manager = mock(RateLimiterConfigManager.class);
        when(manager.resolve("tenant-a", "u1", "/api/test")).thenReturn(new ResolvedRateLimitPolicy(
                "tenant-a",
                "/api/test",
                failureMode,
                List.of(new RateLimitBucket(LimitScope.TENANT, "ratelimex:{tenant-a}:tenant:all", new RateLimitConfig(1, 1, 60)))
        ));
        return manager;
    }

    private static RateLimitStore fixedStore(RateLimitStoreResult result) {
        return (buckets, cost) -> result;
    }

    private static RateLimitStore failingStore() {
        return (buckets, cost) -> {
            throw new RateLimitBackendException("redis down");
        };
    }
}
