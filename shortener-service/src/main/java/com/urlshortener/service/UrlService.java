package com.urlshortener.service;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Core URL shortening service.
 *
 * Flow:
 * 1. CREATE: Generate short code → persist to DB → cache in Redis
 * 2. REDIRECT: Check Redis → fallback to DB → publish click event to Kafka → 302 redirect
 * 3. CLEANUP: Scheduled job deactivates expired URLs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlMappingRepository urlMappingRepository;
    private final CacheService cacheService;
    private final AnalyticsProducer analyticsProducer;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code-length}")
    private int shortCodeLength;

    @Value("${app.default-ttl-days}")
    private int defaultTtlDays;

    /**
     * Create a new shortened URL.
     */
    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, UUID apiKeyId) {
        String shortCode;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            // Custom alias requested
            shortCode = request.getCustomAlias().trim();
            if (urlMappingRepository.existsByShortCode(shortCode)) {
                throw new IllegalArgumentException("Custom alias '" + shortCode + "' is already taken");
            }
        } else {
            // Generate a unique short code
            shortCode = generateUniqueShortCode();
        }

        // Calculate expiration
        int ttlDays = request.getTtlDays() != null ? request.getTtlDays() : defaultTtlDays;
        Instant expiresAt = Instant.now().plus(ttlDays, ChronoUnit.DAYS);

        // Persist to database
        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .longUrl(request.getLongUrl())
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .clickCount(0L)
                .isActive(true)
                .apiKeyId(apiKeyId)
                .build();

        mapping = urlMappingRepository.save(mapping);
        log.info("Created short URL: {} → {}", shortCode, request.getLongUrl());

        // Cache the mapping (write-through)
        Duration cacheTtl = Duration.between(Instant.now(), expiresAt);
        cacheService.cacheUrl(shortCode, request.getLongUrl(), cacheTtl);

        return toResponse(mapping);
    }

    /**
     * Resolve a short code to the original URL.
     * This is the HOT PATH — must be fast.
     */
    public Optional<String> resolveUrl(String shortCode) {
        // 1. Check Redis cache first (fastest)
        String cachedUrl = cacheService.getCachedUrl(shortCode);
        if (cachedUrl != null) {
            return Optional.of(cachedUrl);
        }

        // 2. Fallback to database
        Optional<UrlMapping> mappingOpt = urlMappingRepository.findByShortCodeAndIsActiveTrue(shortCode);

        if (mappingOpt.isPresent()) {
            UrlMapping mapping = mappingOpt.get();

            // Check if expired
            if (mapping.isExpired()) {
                log.info("URL expired: {}", shortCode);
                return Optional.empty();
            }

            // Populate cache for next time (read-through)
            cacheService.cacheUrl(shortCode, mapping.getLongUrl());

            return Optional.of(mapping.getLongUrl());
        }

        return Optional.empty();
    }

    /**
     * Record a click event asynchronously via Kafka.
     */
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referrer) {
        analyticsProducer.publishClickEvent(shortCode, ipAddress, userAgent, referrer);
    }

    /**
     * Get URL details by short code.
     */
    public Optional<ShortenResponse> getUrlDetails(String shortCode) {
        return urlMappingRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .map(this::toResponse);
    }

    /**
     * List all URLs for an API key (paginated).
     */
    public Page<ShortenResponse> listUrls(UUID apiKeyId, Pageable pageable) {
        return urlMappingRepository
                .findByApiKeyIdAndIsActiveTrueOrderByCreatedAtDesc(apiKeyId, pageable)
                .map(this::toResponse);
    }

    /**
     * Deactivate a URL by short code.
     */
    @Transactional
    public boolean deactivateUrl(String shortCode, UUID apiKeyId) {
        Optional<UrlMapping> mappingOpt = urlMappingRepository.findByShortCodeAndIsActiveTrue(shortCode);
        if (mappingOpt.isPresent()) {
            UrlMapping mapping = mappingOpt.get();
            // Verify ownership
            if (!mapping.getApiKeyId().equals(apiKeyId)) {
                throw new SecurityException("You do not own this URL");
            }
            mapping.setIsActive(false);
            urlMappingRepository.save(mapping);
            cacheService.evictUrl(shortCode);
            log.info("Deactivated URL: {}", shortCode);
            return true;
        }
        return false;
    }

    /**
     * Scheduled cleanup: deactivate expired URLs every hour.
     */
    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupExpiredUrls() {
        int count = urlMappingRepository.deactivateExpiredUrls(Instant.now());
        if (count > 0) {
            log.info("Deactivated {} expired URLs", count);
        }
    }

    // ─── Private Helpers ──────────────────────────────────

    /**
     * Generate a unique random Base62 short code.
     * Uses SecureRandom for unpredictable codes with collision checking.
     * With 7-char Base62, there are 62^7 ≈ 3.5 trillion possibilities.
     */
    private String generateUniqueShortCode() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int maxAttempts = 10;

        for (int i = 0; i < maxAttempts; i++) {
            StringBuilder sb = new StringBuilder(shortCodeLength);
            for (int j = 0; j < shortCodeLength; j++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String shortCode = sb.toString();

            if (!urlMappingRepository.existsByShortCode(shortCode)) {
                return shortCode;
            }
            log.warn("Short code collision detected: {}, retrying...", shortCode);
        }
        throw new RuntimeException("Failed to generate unique short code after " + maxAttempts + " attempts");
    }

    private ShortenResponse toResponse(UrlMapping mapping) {
        return ShortenResponse.builder()
                .shortCode(mapping.getShortCode())
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .longUrl(mapping.getLongUrl())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .clickCount(mapping.getClickCount())
                .build();
    }
}
