package com.anandorg.ratelimex.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminApiKeyAuthenticationFilter.class);
    private static final List<SimpleGrantedAuthority> ADMIN_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

    private final AdminSecurityProperties properties;

    public AdminApiKeyAuthenticationFilter(AdminSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!requiresAdmin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!properties.hasConfiguredAdminKey()) {
            log.warn("Admin API key is not configured; rejecting admin request to {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Admin API key is not configured");
            return;
        }

        String providedKey = request.getHeader(properties.getAdminApiKeyHeader());
        if (!matches(providedKey, properties.getAdminApiKey())) {
            log.warn("Rejected admin request to {} due to missing or invalid API key", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid admin API key");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin-api-key",
                null,
                ADMIN_AUTHORITIES
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static boolean requiresAdmin(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/admin")
                || path.startsWith("/admin/")
                || path.equals("/actuator/info")
                || path.equals("/actuator/metrics")
                || path.startsWith("/actuator/metrics/");
    }

    private static boolean matches(String providedKey, String configuredKey) {
        if (providedKey == null || configuredKey == null) {
            return false;
        }
        byte[] providedBytes = providedKey.getBytes(StandardCharsets.UTF_8);
        byte[] configuredBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(providedBytes, configuredBytes);
    }
}
