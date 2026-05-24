package com.anandorg.ratelimex.controller;

import java.net.URI;
import java.util.List;

import com.anandorg.ratelimex.dto.TenantApiPolicyRequest;
import com.anandorg.ratelimex.dto.TenantApiPolicyResponse;
import com.anandorg.ratelimex.model.TenantApiPolicy;
import com.anandorg.ratelimex.service.policy.TenantPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/admin/tenants")
public class AdminTenantPolicyController {

    private final TenantPolicyService tenantPolicyService;

    public AdminTenantPolicyController(TenantPolicyService tenantPolicyService) {
        this.tenantPolicyService = tenantPolicyService;
    }

    @GetMapping("/{tenantId}/apis")
    public List<TenantApiPolicyResponse> list(@PathVariable String tenantId) {
        return tenantPolicyService.listTenantPolicies(tenantId)
                .stream()
                .map(TenantApiPolicyResponse::from)
                .toList();
    }

    @PostMapping("/{tenantId}/apis")
    public ResponseEntity<TenantApiPolicyResponse> upsert(
            @PathVariable String tenantId,
            @RequestBody TenantApiPolicyRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        TenantApiPolicy saved = tenantPolicyService.upsertPolicy(request.toPolicy(tenantId));
        URI location = uriBuilder.path("/admin/tenants/{tenantId}/apis")
                .queryParam("api", saved.api())
                .build(saved.tenantId());
        return ResponseEntity.created(location).body(TenantApiPolicyResponse.from(saved));
    }

    @DeleteMapping("/{tenantId}/apis")
    public ResponseEntity<Void> delete(@PathVariable String tenantId, @RequestParam String api) {
        tenantPolicyService.deletePolicy(tenantId, api);
        return ResponseEntity.noContent().build();
    }
}
