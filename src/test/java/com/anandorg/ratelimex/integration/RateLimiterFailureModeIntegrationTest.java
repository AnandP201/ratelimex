package com.anandorg.ratelimex.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "ratelimex.security.admin-api-key=test-admin-key",
        "ratelimex.namespace=redis-down-integration",
        "spring.data.redis.timeout=100ms"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RateLimiterFailureModeIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ratelimex")
            .withUsername("ratelimex")
            .withPassword("ratelimex");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> 1);
    }

    @Test
    void redisDownWithFailOpenAllowsRequestInDegradedMode() throws Exception {
        createTenantPolicy("fail-open-tenant", "/reports/export", "FAIL_OPEN");

        rateLimitCheck("fail-open-tenant", "/reports/export")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.degraded").value(true))
                .andExpect(jsonPath("$.reason").value("backend_unavailable_fail_open"))
                .andExpect(header().string("X-RateLimit-Degraded", "true"));
    }

    @Test
    void redisDownWithFailClosedBlocksRequestInDegradedMode() throws Exception {
        createTenantPolicy("fail-closed-tenant", "/payments/charge", "FAIL_CLOSED");

        rateLimitCheck("fail-closed-tenant", "/payments/charge")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.degraded").value(true))
                .andExpect(jsonPath("$.reason").value("backend_unavailable_fail_closed"))
                .andExpect(header().string("X-RateLimit-Degraded", "true"));
    }

    private void createTenantPolicy(String tenantId, String api, String failureMode) throws Exception {
        String policy = """
                {
                  "api": "%s",
                  "enabled": true,
                  "failureMode": "%s",
                  "tenantLimit": {
                    "capacity": 100,
                    "refillTokensPerSecond": 10,
                    "ttlSeconds": 120
                  },
                  "apiLimit": {
                    "capacity": 100,
                    "refillTokensPerSecond": 10,
                    "ttlSeconds": 120
                  },
                  "userLimit": {
                    "capacity": 100,
                    "refillTokensPerSecond": 10,
                    "ttlSeconds": 120
                  }
                }
                """.formatted(api, failureMode);

        mockMvc.perform(post("/admin/tenants/{tenantId}/apis", tenantId)
                        .header("X-Admin-Api-Key", "test-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policy))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.api").value(api))
                .andExpect(jsonPath("$.failureMode").value(failureMode));
    }

    private org.springframework.test.web.servlet.ResultActions rateLimitCheck(String tenantId, String api) throws Exception {
        String request = """
                {
                  "tenantId": "%s",
                  "userId": "user-123",
                  "api": "%s",
                  "cost": 1
                }
                """.formatted(tenantId, api);

        return mockMvc.perform(post("/api/rate-limit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }
}
