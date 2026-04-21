package com.urlshortener.controller;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.model.ApiKey;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "URL shortening and redirect operations")
public class UrlController {

    private final UrlService urlService;
    private final RateLimiterService rateLimiterService;

    /**
     * Create a new short URL.
     * Requires X-API-Key header.
     */
    @PostMapping("/api/v1/shorten")
    @Operation(summary = "Shorten a URL", description = "Create a new shortened URL with optional custom alias and TTL")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {

        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("resolvedApiKey");

        // Rate limiting
        if (!rateLimiterService.isAllowed(apiKey.getApiKey(), apiKey.getRateLimit())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again later.\"}");
        }

        ShortenResponse response = urlService.shortenUrl(request, apiKey.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Redirect to the original URL.
     * This is the HOT PATH — public, no auth required.
     */
    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect", description = "Redirect to the original URL for the given short code")
    public void redirect(
            @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Skip API and internal paths
        if (shortCode.startsWith("api") || shortCode.startsWith("swagger") ||
            shortCode.startsWith("v3") || shortCode.equals("favicon.ico")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Optional<String> longUrlOpt = urlService.resolveUrl(shortCode);

        if (longUrlOpt.isPresent()) {
            // Record click event async (doesn't block the redirect)
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String referrer = request.getHeader("Referer");
            urlService.recordClick(shortCode, ipAddress, userAgent, referrer);

            // 302 redirect
            response.sendRedirect(longUrlOpt.get());
            log.debug("Redirected {} → {}", shortCode, longUrlOpt.get());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Short URL not found or expired\"}"
            );
        }
    }

    /**
     * Get URL details.
     */
    @GetMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Get URL details", description = "Get details and stats for a shortened URL")
    public ResponseEntity<?> getUrlDetails(@PathVariable String shortCode) {
        Optional<ShortenResponse> response = urlService.getUrlDetails(shortCode);
        return response
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all URLs for the authenticated API key.
     */
    @GetMapping("/api/v1/urls")
    @Operation(summary = "List URLs", description = "List all shortened URLs for the authenticated API key")
    public ResponseEntity<Page<ShortenResponse>> listUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        ApiKey apiKey = (ApiKey) request.getAttribute("resolvedApiKey");
        Page<ShortenResponse> urls = urlService.listUrls(apiKey.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(urls);
    }

    /**
     * Deactivate a URL.
     */
    @DeleteMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Delete URL", description = "Deactivate a shortened URL")
    public ResponseEntity<?> deleteUrl(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        ApiKey apiKey = (ApiKey) request.getAttribute("resolvedApiKey");
        boolean deleted = urlService.deactivateUrl(shortCode, apiKey.getId());
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ─── Helpers ──────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
