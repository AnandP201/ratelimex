package com.anandorg.ratelimex.controller;

import java.net.URI;
import java.util.List;

import com.anandorg.ratelimex.dto.TenantApiPolicyRequest;
import com.anandorg.ratelimex.dto.TenantApiPolicyResponse;
import com.anandorg.ratelimex.config.OpenApiConfig;
import com.anandorg.ratelimex.model.TenantApiPolicy;
import com.anandorg.ratelimex.service.policy.TenantPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Admin Section", description = "Contains APIs for managing rate-limited, tenant specific, API policies")
@SecurityRequirement(name = OpenApiConfig.ADMIN_API_KEY_SCHEME)
public class AdminTenantPolicyController {

    private final TenantPolicyService tenantPolicyService;

    public AdminTenantPolicyController(TenantPolicyService tenantPolicyService) {
        this.tenantPolicyService = tenantPolicyService;
    }

    @GetMapping("/{tenantId}/apis")
    @Operation(summary="Get all API policies for a tenant", description = "tenantId required in route variable")
    public List<TenantApiPolicyResponse> list(@PathVariable String tenantId) {
        return tenantPolicyService.listTenantPolicies(tenantId)
                .stream()
                .map(TenantApiPolicyResponse::from)
                .toList();
    }

    @PostMapping("/{tenantId}/apis")
    @Operation(summary="Create a new API policy for a tenant", description = "tenantId required in route variable")
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
    @Operation(summary="Delete an API policy", description = "tenantId required in route variable and API name required in query param")
    public ResponseEntity<Void> delete(@PathVariable String tenantId, @RequestParam String api) {
        tenantPolicyService.deletePolicy(tenantId, api);
        return ResponseEntity.noContent().build();
    }
}
