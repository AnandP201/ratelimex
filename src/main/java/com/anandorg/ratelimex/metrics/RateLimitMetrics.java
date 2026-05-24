package com.anandorg.ratelimex.metrics;

import java.util.concurrent.Callable;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class RateLimitMetrics {

    private final MeterRegistry meterRegistry;

    public RateLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDecision(boolean allowed, boolean degraded, String reason) {
        meterRegistry.counter(
                "ratelimit.decisions",
                "outcome", allowed ? "allowed" : "blocked",
                "degraded", Boolean.toString(degraded),
                "reason", (reason == null || reason.isBlank()? "unknown" : reason))
                .increment();
    }

    public <T> T recordRedisLatency(Callable<T> operation) throws Exception {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return operation.call();
        } finally {
            sample.stop(meterRegistry.timer("ratelimit.redis.lua.latency"));
        }
    }
}
