package com.anandorg.ratelimex.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "ratelimex.security.admin-api-key=test-admin-key",
        "ratelimex.namespace=integration"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RateLimiterEndToEndIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ratelimex")
            .withUsername("ratelimex")
            .withPassword("ratelimex");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void adminPolicyCreationAndRedisRateLimitingWorkEndToEnd() throws Exception {
        createTenantPolicy("acme", "/payments/charge", "FAIL_CLOSED", 2);

        rateLimitCheck("acme", "user-123", "/payments/charge").andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.reason").value("allowed"))
                .andExpect(header().string("X-RateLimit-Degraded", "false"));

        rateLimitCheck("acme", "user-123", "/payments/charge").andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.reason").value("allowed"));

        rateLimitCheck("acme", "user-123", "/payments/charge").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.reason").value("rate_limited"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    @Test
    void tenantsHaveIsolatedBucketsForTheSameApiAndUser() throws Exception {
        createTenantPolicy("tenant-a", "/shared/search", "FAIL_CLOSED", 1);
        createTenantPolicy("tenant-b", "/shared/search", "FAIL_CLOSED", 1);

        // tenant-a - user-name same - first request
        rateLimitCheck("tenant-a", "same-user", "/shared/search")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));

        rateLimitCheck("tenant-a", "same-user", "/shared/search")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("rate_limited"));

        rateLimitCheck("tenant-b", "same-user", "/shared/search")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.reason").value("allowed"));
    }

    private void createTenantPolicy(String tenantId, String api, String failureMode, int apiCapacity) throws Exception {
        String policy = """
                {
                  "api": "%s",
                  "enabled": true,
                  "failureMode": "%s",
                  "tenantLimit": {
                    "capacity": 100,
                    "refillTokensPerSecond": 0,
                    "ttlSeconds": 120
                  },
                  "apiLimit": {
                    "capacity": %d,
                    "refillTokensPerSecond": 0,
                    "ttlSeconds": 120
                  },
                  "userLimit": {
                    "capacity": 100,
                    "refillTokensPerSecond": 0,
                    "ttlSeconds": 120
                  }
                }
                """.formatted(api, failureMode, apiCapacity);

        mockMvc.perform(post("/admin/tenants/{tenantId}/apis", tenantId)
                        .header("X-Admin-Api-Key", "test-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policy))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.api").value(api))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions rateLimitCheck(String tenantId, String userId, String api) throws Exception {
        String request = """
                {
                  "tenantId": "%s",
                  "userId": "%s",
                  "api": "%s",
                  "cost": 1
                }
                """.formatted(tenantId, userId, api);

        return mockMvc.perform(post("/api/rate-limit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }
}
