package com.anandorg.ratelimex.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ADMIN_API_KEY_SCHEME = "adminApiKey";

    @Bean
    public OpenAPI ratelimexOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ratelimex API")
                        .version("v1")
                        .description("Distributed tenant-enabled rate-limiter service"))
                .components(new Components()
                        .addSecuritySchemes(ADMIN_API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Admin-Api-Key")));
    }
}
