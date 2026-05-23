package com.anandorg.ratelimex.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.LimitScope;
import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.RateLimitConfig;
import com.anandorg.ratelimex.model.ResolvedRateLimitPolicy;
import com.anandorg.ratelimex.model.TenantApiPolicy;
import com.anandorg.ratelimex.service.policy.TenantPolicyService;
import org.junit.jupiter.api.Test;

class RateLimiterConfigManagerTest {

    @Test
    void buildsTenantApiAndUserBucketsWithClusterSafeTenantHashTag() {

        RatelimexProperties properties = new RatelimexProperties();
        properties.setNamespace("prod-a");

        TenantPolicyService policyService = mock(TenantPolicyService.class);
        when(policyService.findPolicy("tenant-a", "/api/search")).
                thenReturn(Optional.of(policy()));

        RateLimiterConfigManager manager = new RateLimiterConfigManager(properties, policyService);

        ResolvedRateLimitPolicy resolved = manager.resolve(
                "tenant-a", "user-123", "/api/search");

        assertThat(resolved.failureMode()).isEqualTo(FailureMode.FAIL_CLOSED);
        assertThat(resolved.buckets()).extracting(RateLimitBucket::scope)
                .containsExactly(LimitScope.TENANT, LimitScope.API, LimitScope.USER);
        assertThat(resolved.buckets()).allSatisfy(bucket -> assertThat(bucket.key()).contains("ratelimex:{prod-a:"));
        assertThat(resolved.buckets().get(0).config().getCapacity()).isEqualTo(500);
        assertThat(resolved.buckets().get(1).config().getCapacity()).isEqualTo(100);
        assertThat(resolved.buckets().get(2).config().getCapacity()).isEqualTo(20);
    }

    private static TenantApiPolicy policy() {
        return new TenantApiPolicy(
                "tenant-a",
                "/api/search",
                true,
                FailureMode.FAIL_CLOSED,
                new RateLimitConfig(500, 50, 120),
                new RateLimitConfig(100, 10, 120),
                new RateLimitConfig(20, 2, 120),
                0
        );
    }
}
