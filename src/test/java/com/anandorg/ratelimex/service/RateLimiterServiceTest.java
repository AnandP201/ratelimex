package com.anandorg.ratelimex.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.anandorg.ratelimex.config.RatelimexProperties;
import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.repository.RateLimitBackendException;
import com.anandorg.ratelimex.service.strategy.RateLimitStrategy;
import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

    @Test
    void failsClosedWhenRedisIsUnavailableAndPolicySaysSo() {
        RatelimexProperties properties = new RatelimexProperties();
        properties.setFailureMode(FailureMode.FAIL_CLOSED);
        RateLimiterService service = new RateLimiterService(failingStrategy(), properties);

        RateLimitDecision decision = service.allowRequest("u1", "/api/test");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.degraded()).isTrue();
        assertThat(decision.reason()).isEqualTo("backend_unavailable_fail_closed");
    }

    @Test
    void failsOpenWhenRedisIsUnavailableAndPolicySaysSo() {
        RatelimexProperties properties = new RatelimexProperties();
        properties.setFailureMode(FailureMode.FAIL_OPEN);
        RateLimiterService service = new RateLimiterService(failingStrategy(), properties);

        RateLimitDecision decision = service.allowRequest("u1", "/api/test");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.degraded()).isTrue();
        assertThat(decision.reason()).isEqualTo("backend_unavailable_fail_open");
    }

    private static RateLimitStrategy failingStrategy() {
        return (userId, api, cost) -> {
            throw new RateLimitBackendException("redis down");
        };
    }
}
