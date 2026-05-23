package com.anandorg.ratelimex.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.anandorg.ratelimex.model.LimitScope;
import com.anandorg.ratelimex.model.RateLimitBucket;
import com.anandorg.ratelimex.model.ResolvedRateLimitPolicy;
import com.anandorg.ratelimex.model.TenantApiPolicy;
import com.anandorg.ratelimex.service.policy.RateLimitPolicyException;
import com.anandorg.ratelimex.service.policy.TenantPolicyService;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterConfigManager {

    private final RatelimexProperties properties;
    private final TenantPolicyService tenantPolicyService;

    public RateLimiterConfigManager(RatelimexProperties properties, TenantPolicyService tenantPolicyService) {
        this.properties = properties;
        this.tenantPolicyService = tenantPolicyService;
    }

    public ResolvedRateLimitPolicy resolve(String tenantId, String userId, String api) {
        String normalizedTenant = normalizeRequired("tenantId", tenantId);
        String normalizedUser = normalizeRequired("userId", userId);
        String normalizedApi = normalizeRequired("api", api);


        TenantApiPolicy policy = tenantPolicyService.findPolicy(normalizedTenant, normalizedApi)
                .orElseThrow(() -> new RateLimitPolicyException(
                        "api_not_enabled_for_tenant",
                        "No enabled API policy exists for tenant " + normalizedTenant + " and api " + normalizedApi
                ));

        if (!policy.enabled()) {
            throw new RateLimitPolicyException(
                    "api_not_enabled_for_tenant",
                    "API " + normalizedApi + " is disabled for tenant " + normalizedTenant
            );
        }

        policy.validate();

        return new ResolvedRateLimitPolicy(
                normalizedTenant,
                normalizedApi,
                policy.failureMode(),
                List.of(
                        new RateLimitBucket(LimitScope.TENANT, redisKey(normalizedTenant, "tenant", "all"), policy.tenantLimit()),
                        new RateLimitBucket(LimitScope.API, redisKey(normalizedTenant, "api", hash(normalizedApi)), policy.apiLimit()),
                        new RateLimitBucket(LimitScope.USER, redisKey(normalizedTenant, "user", hash(normalizedUser)), policy.userLimit())
                )
        );
    }

    private String redisKey(String tenantId, String scope, String id) {
        String namespace = normalizeRequired("ratelimex.namespace", properties.getNamespace());
        String hashTag = namespace + ":" + hash(tenantId);
        return "ratelimex:{" + hashTag + "}:" + scope + ":" + id;
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
