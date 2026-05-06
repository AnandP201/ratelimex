package com.anandorg.ratelimex.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.anandorg.ratelimex.model.LimitScope;
import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.RateLimitConfig;
import org.junit.jupiter.api.Test;

class RateLimiterConfigManagerTest {

    @Test
    void buildsGlobalApiAndUserBucketsWithClusterSafeHashTag() {
        RatelimexProperties properties = new RatelimexProperties();
        properties.setNamespace("prod-a");
        properties.getApiLimits().put("/api/search", new RateLimitConfig(20, 5));

        RateLimiterConfigManager manager = new RateLimiterConfigManager(properties);

        List<RateLimitBucket> buckets = manager.bucketsFor("user-123", "/api/search");

        assertThat(buckets).extracting(RateLimitBucket::scope)
                .containsExactly(LimitScope.GLOBAL, LimitScope.API, LimitScope.USER);
        assertThat(buckets).allSatisfy(bucket -> assertThat(bucket.key()).contains("ratelimex:{prod-a}:"));
        assertThat(buckets.get(1).config().getCapacity()).isEqualTo(20);
        assertThat(buckets.get(2).config().getCapacity()).isEqualTo(60);
    }
}
