package com.stockbridge.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Filter that enforces API-key authentication on all requests matching /api/**.
 * Excludes OPTIONS (CORS preflight) requests and /actuator/health.
 * Executes prior to the JWT authentication filter.
 */
@Slf4j
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";

    @Value("${backend.api-key:}")
    private String configuredApiKey;

    @PostConstruct
    public void validateConfiguration() {
        if (configuredApiKey == null || configuredApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "FATAL CONFIGURATION ERROR: Environment variable BACKEND_API_KEY is not set. Application startup aborted."
            );
        }
        log.info("API Key authentication filter initialized successfully.");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Exclude CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // Exclude Actuator health endpoint
        if (uri.startsWith("/actuator/health")) {
            return true;
        }

        // Only enforce API-key on /api/**
        return !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(API_KEY_HEADER);

        if (providedKey == null || !isValidKey(providedKey)) {
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidKey(String providedKey) {
        byte[] providedBytes = providedKey.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = configuredApiKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(providedBytes, expectedBytes);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"Invalid API key\"}");
    }
}
