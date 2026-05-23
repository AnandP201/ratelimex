package com.anandorg.ratelimex.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.anandorg.ratelimex.metrics.RateLimitMetrics;
import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.RateLimitDecision;
import com.anandorg.ratelimex.service.policy.RateLimitPolicyException;
import com.anandorg.ratelimex.service.strategy.RateLimitStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RateLimiterServiceTest {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterServiceTest.class);

    @Test
    void rejectsWhenTenantApiPolicyIsMissingOrDisabled() {

        RateLimiterService service = new RateLimiterService(
                policyRejectingStrategy(),
                new RateLimitMetrics(new SimpleMeterRegistry())
        );

        RateLimitDecision decision = service.allowRequest("tenant-a", "u1", "/api/private");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.degraded()).isFalse();
        assertThat(decision.reason()).isEqualTo("api_not_enabled_for_tenant");
    }

    @Test
    void notAllowedWhenDegradedAndFailClosedConfig(){
        RateLimiterService service = new RateLimiterService(
                degradedServiceFailClosed(),
                new RateLimitMetrics(new SimpleMeterRegistry())
        );

        RateLimitDecision decision = service.allowRequest("tenant-b","user-2","api/test");


        assertThat(decision.degraded()).isTrue();
        assertThat(decision.reason()).isEqualTo("backend_unavailable_fail_closed");
        assertThat(decision.allowed()).isFalse();
    }

    @Test
    void allowedWhenDegradedAndFailOpenConfig(){
        RateLimiterService service = new RateLimiterService(
                degradedServiceFailOpen(),
                new RateLimitMetrics(new SimpleMeterRegistry())
        );

        RateLimitDecision decision = service.allowRequest("tenant-b","user-2","api/test");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.degraded()).isTrue();
        assertThat(decision.reason()).isEqualTo("backend_unavailable_fail_open");
        assertThat(decision.remainingTokens()).isEqualTo(0);
    }

    private static RateLimitStrategy policyRejectingStrategy() {
        return (tenantId, userId, api, cost) -> {
            throw new RateLimitPolicyException("api_not_enabled_for_tenant", "disabled");
        };
    }

    private static RateLimitStrategy degradedServiceFailClosed() {
        return (tenantId, userId, api, cost) -> RateLimitDecision.degraded(FailureMode.FAIL_CLOSED);
    }

    private static RateLimitStrategy degradedServiceFailOpen() {
        return (tenantId, userId, api, cost) -> RateLimitDecision.degraded(FailureMode.FAIL_OPEN);
    }
}
