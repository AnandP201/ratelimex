package com.anandorg.ratelimex.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "ratelimex.security.admin-api-key=test-admin-key"
})
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminApiRejectsMissingApiKey() throws Exception {
        mockMvc.perform(get("/admin/tenants/acme/apis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminApiRejectsInvalidApiKey() throws Exception {
        mockMvc.perform(get("/admin/tenants/acme/apis")
                        .header("X-Admin-Api-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminApiAllowsValidApiKey() throws Exception {
        mockMvc.perform(get("/admin/tenants/acme/apis")
                        .header("X-Admin-Api-Key", "test-admin-key"))
                .andExpect(status().isOk());
    }

    @Test
    void rateLimitCheckDoesNotRequireAdminApiKey() throws Exception {
        String body = """
                {
                  "tenantId": "acme",
                  "userId": "user-123",
                  "api": "/payments/charge",
                  "cost": 1
                }
                """;

        mockMvc.perform(post("/api/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void livenessHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsEndpointRequiresAdminApiKey() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsEndpointAllowsValidAdminApiKey() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("X-Admin-Api-Key", "test-admin-key"))
                .andExpect(status().isOk());
    }
}
