package com.anandorg.ratelimex.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.RateLimitConfig;
import com.anandorg.ratelimex.model.LimitScope;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterConfigManager {

    private final RatelimexProperties properties;

    public RateLimiterConfigManager(RatelimexProperties properties) {
        this.properties = properties;
    }

    public List<RateLimitBucket> bucketsFor(String userId, String api) {

        String normalizedUser = normalizeRequired("userId", userId);
        String normalizedApi = normalizeRequired("api", api);

        RateLimitConfig globalLimit = properties.getGlobalLimit();
        RateLimitConfig apiLimit = properties.getApiLimits().getOrDefault(normalizedApi, properties.getDefaultApiLimit());
        RateLimitConfig userLimit = properties.getUserLimits().getOrDefault(normalizedUser, properties.getDefaultUserLimit());

        globalLimit.validate("ratelimex.globalLimit");
        apiLimit.validate("ratelimex.apiLimit[" + normalizedApi + "]");
        userLimit.validate("ratelimex.userLimit[" + normalizedUser + "]");

        return List.of(
                new RateLimitBucket(LimitScope.GLOBAL, redisKey("global", "all"), globalLimit),
                new RateLimitBucket(LimitScope.API, redisKey("api", hash(normalizedApi)), apiLimit),
                new RateLimitBucket(LimitScope.USER, redisKey("user", hash(normalizedUser)), userLimit)
        );
    }

    private String redisKey(String scope, String id) {
        String namespace = normalizeRequired("ratelimex.namespace", properties.getNamespace());
        return "ratelimex:{" + namespace + "}:" + scope + ":" + id;
    }

    private static String normalizeRequired(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
