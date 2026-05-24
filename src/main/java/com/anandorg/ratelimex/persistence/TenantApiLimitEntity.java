package com.anandorg.ratelimex.persistence;

import java.time.Instant;

import com.anandorg.ratelimex.model.FailureMode;
import com.anandorg.ratelimex.model.RateLimitConfig;
import com.anandorg.ratelimex.model.TenantApiPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "tenant_api_limits",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenant_api_limits_tenant_api", columnNames = {"tenant_id", "api_path"})
)
public class TenantApiLimitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "api_path", nullable = false, length = 512)
    private String api;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_mode", nullable = false, length = 32)
    private FailureMode failureMode = FailureMode.FAIL_CLOSED;

    @Column(name = "tenant_capacity", nullable = false)
    private int tenantCapacity;

    @Column(name = "tenant_refill_tokens_per_second", nullable = false)
    private double tenantRefillTokensPerSecond;

    @Column(name = "tenant_ttl_seconds", nullable = false)
    private int tenantTtlSeconds;

    @Column(name = "api_capacity", nullable = false)
    private int apiCapacity;

    @Column(name = "api_refill_tokens_per_second", nullable = false)
    private double apiRefillTokensPerSecond;

    @Column(name = "api_ttl_seconds", nullable = false)
    private int apiTtlSeconds;

    @Column(name = "user_capacity", nullable = false)
    private int userCapacity;

    @Column(name = "user_refill_tokens_per_second", nullable = false)
    private double userRefillTokensPerSecond;

    @Column(name = "user_ttl_seconds", nullable = false)
    private int userTtlSeconds;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TenantApiPolicy toPolicy() {
        return new TenantApiPolicy(
                tenantId,
                api,
                enabled,
                failureMode,
                new RateLimitConfig(tenantCapacity, tenantRefillTokensPerSecond, tenantTtlSeconds),
                new RateLimitConfig(apiCapacity, apiRefillTokensPerSecond, apiTtlSeconds),
                new RateLimitConfig(userCapacity, userRefillTokensPerSecond, userTtlSeconds),
                version
        );
    }

    public void applyPolicy(TenantApiPolicy policy) {
        this.tenantId = policy.tenantId();
        this.api = policy.api();
        this.enabled = policy.enabled();
        this.failureMode = policy.failureMode();
        this.tenantCapacity = policy.tenantLimit().getCapacity();
        this.tenantRefillTokensPerSecond = policy.tenantLimit().getRefillTokensPerSecond();
        this.tenantTtlSeconds = policy.tenantLimit().getTtlSeconds();
        this.apiCapacity = policy.apiLimit().getCapacity();
        this.apiRefillTokensPerSecond = policy.apiLimit().getRefillTokensPerSecond();
        this.apiTtlSeconds = policy.apiLimit().getTtlSeconds();
        this.userCapacity = policy.userLimit().getCapacity();
        this.userRefillTokensPerSecond = policy.userLimit().getRefillTokensPerSecond();
        this.userTtlSeconds = policy.userLimit().getTtlSeconds();
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getApi() {
        return api;
    }
}
