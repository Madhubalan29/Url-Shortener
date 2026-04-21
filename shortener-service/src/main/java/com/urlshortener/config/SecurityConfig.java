package com.urlshortener.config;

import com.urlshortener.model.ApiKey;
import com.urlshortener.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * API key authentication filter.
 * 
 * - Redirect endpoint (GET /{shortCode}) is PUBLIC — no auth needed
 * - All /api/* endpoints require a valid X-API-Key header
 * - The resolved ApiKey entity is stored as a request attribute
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyRepository apiKeyRepository;

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration() {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiKeyFilter(apiKeyRepository));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    public static class ApiKeyFilter extends OncePerRequestFilter {

        private final ApiKeyRepository apiKeyRepository;

        public ApiKeyFilter(ApiKeyRepository apiKeyRepository) {
            this.apiKeyRepository = apiKeyRepository;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            // Allow Swagger/OpenAPI docs without auth
            String path = request.getRequestURI();
            if (path.startsWith("/api/docs") || path.startsWith("/api/swagger-ui") ||
                path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Allow API key creation endpoint without auth (bootstrap)
            if (path.equals("/api/v1/auth/keys") && "POST".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String apiKeyHeader = request.getHeader("X-API-Key");
            if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Missing X-API-Key header\"}"
                );
                return;
            }

            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKeyAndIsActiveTrue(apiKeyHeader);
            if (apiKeyOpt.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or inactive API key\"}"
                );
                return;
            }

            // Store the resolved API key in request attributes for downstream use
            request.setAttribute("resolvedApiKey", apiKeyOpt.get());
            filterChain.doFilter(request, response);
        }
    }
}
