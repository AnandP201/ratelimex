package com.anandorg.ratelimex.service.policy;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.anandorg.ratelimex.model.TenantApiPolicy;
import com.anandorg.ratelimex.persistence.TenantApiLimitEntity;
import com.anandorg.ratelimex.persistence.TenantApiLimitRepository;
import com.anandorg.ratelimex.service.RateLimiterService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantPolicyService {

    private static final Logger log = LoggerFactory.getLogger(TenantPolicyService.class);

    private final TenantApiLimitRepository repository;
    private final Cache<PolicyKey, Optional<TenantApiPolicy>> cache;

    // constructor level injection
    public TenantPolicyService(TenantApiLimitRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<TenantApiPolicy> findPolicy(String tenantId, String api) {
        PolicyKey key = new PolicyKey(normalize("tenantId", tenantId), normalize("api", api));
        //Set<PolicyKey> set= cache.asMap().keySet();
        //log.info("Cached keys: {}", set);
        return cache.get(key, ignored -> repository.findByTenantIdAndApi(key.tenantId(), key.api())
                .map(TenantApiLimitEntity::toPolicy));
    }

    @Transactional(readOnly = true)
    public List<TenantApiPolicy> listTenantPolicies(String tenantId) {
        String normalizedTenant = normalize("tenantId", tenantId);
        return repository.findByTenantIdOrderByApiAsc(normalizedTenant)
                .stream()
                .map(TenantApiLimitEntity::toPolicy)
                .toList();
    }

    @Transactional
    public TenantApiPolicy upsertPolicy(TenantApiPolicy policy) {
        policy.validate();
        String tenantId = normalize("tenantId", policy.tenantId());
        String api = normalize("api", policy.api());
        TenantApiPolicy normalizedPolicy = new TenantApiPolicy(
                tenantId,
                api,
                policy.enabled(),
                policy.failureMode(),
                policy.tenantLimit(),
                policy.apiLimit(),
                policy.userLimit(),
                policy.version()
        );

        TenantApiLimitEntity entity = repository.findByTenantIdAndApi(tenantId, api)
                .orElseGet(TenantApiLimitEntity::new);
        entity.applyPolicy(normalizedPolicy);
        TenantApiPolicy saved = repository.save(entity).toPolicy();
        cache.invalidate(new PolicyKey(tenantId, api));
        return saved;
    }

    @Transactional
    public void deletePolicy(String tenantId, String api) {
        String normalizedTenant = normalize("tenantId", tenantId);
        String normalizedApi = normalize("api", api);
        repository.deleteByTenantIdAndApi(normalizedTenant, normalizedApi);
        cache.invalidate(new PolicyKey(normalizedTenant, normalizedApi));
    }

    public void invalidate(String tenantId, String api) {
        cache.invalidate(new PolicyKey(normalize("tenantId", tenantId), normalize("api", api)));
    }

    private static String normalize(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private record PolicyKey(String tenantId, String api) { }
}
