package com.anandorg.ratelimex.repository;

import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.RateLimitConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RedisRateLimiterRepository implements RateLimitStore {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final DefaultRedisScript<List> tokenBucketScript;

    public RedisRateLimiterRepository(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/token_bucket_all.lua")));
        this.tokenBucketScript.setResultType(List.class);
    }

    @Override
    public RateLimitStoreResult consume(List<RateLimitBucket> buckets, int cost) {

        if (buckets.isEmpty()) {
            return new RateLimitStoreResult(true, Long.MAX_VALUE, 0);
        }

        if (cost <= 0) {
            throw new IllegalArgumentException("cost must be greater than zero");
        }

        // ratelimex:{default}:{scope}:{id} : redis keys
        List<String> keys = buckets.stream()
                .map(RateLimitBucket::key)
                .toList();

        List<String> args = new ArrayList<>();

        args.add(Long.toString(clock.millis()));
        args.add(Integer.toString(cost));

        for (RateLimitBucket bucket : buckets) {
            RateLimitConfig config = bucket.config();
            args.add(Integer.toString(config.getCapacity()));
            args.add(Double.toString(config.getRefillTokensPerSecond()));
            args.add(Integer.toString(config.getTtlSeconds()));
        }

        try {
            List<?> result = redisTemplate.execute(tokenBucketScript, keys, args.toArray(String[]::new));

            if (result == null || result.size() < 3) {
                throw new RateLimitBackendException("Redis token bucket script returned no result");
            }
            return new RateLimitStoreResult(
                    asLong(result.get(0)) == 1,
                    asLong(result.get(1)),
                    asLong(result.get(2))
            );

        } catch (RedisConnectionFailureException ex) {
            throw new RateLimitBackendException("Redis is unavailable", ex);
        } catch (RuntimeException ex) {
            if (ex instanceof RateLimitBackendException) {
                throw ex;
            }
            throw new RateLimitBackendException("Redis token bucket script failed", ex);
        }
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
