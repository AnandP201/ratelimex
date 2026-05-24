package com.anandorg.ratelimex.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantApiLimitRepository extends JpaRepository<TenantApiLimitEntity, Long> {

    Optional<TenantApiLimitEntity> findByTenantIdAndApi(String tenantId, String api);

    List<TenantApiLimitEntity> findByTenantIdOrderByApiAsc(String tenantId);

    void deleteByTenantIdAndApi(String tenantId, String api);
}
