package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis caching layer for URL mappings.
 *
 * Cache strategy:
 * - Write-through: cache is populated on URL creation
 * - Read-through: on redirect, check cache first, fallback to DB
 * - TTL-based: cache entries expire to prevent stale data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    private static final String URL_CACHE_PREFIX = "url:";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    /**
     * Cache a short code → long URL mapping.
     */
    public void cacheUrl(String shortCode, String longUrl) {
        String key = URL_CACHE_PREFIX + shortCode;
        try {
            redisTemplate.opsForValue().set(key, longUrl, DEFAULT_CACHE_TTL);
            log.debug("Cached URL mapping: {} → {}", shortCode, longUrl);
        } catch (Exception e) {
            log.warn("Failed to cache URL mapping for {}: {}", shortCode, e.getMessage());
            // Cache failures should not break the service
        }
    }

    /**
     * Cache a short code → long URL mapping with a custom TTL.
     */
    public void cacheUrl(String shortCode, String longUrl, Duration ttl) {
        String key = URL_CACHE_PREFIX + shortCode;
        try {
            redisTemplate.opsForValue().set(key, longUrl, ttl);
            log.debug("Cached URL mapping: {} → {} (TTL: {})", shortCode, longUrl, ttl);
        } catch (Exception e) {
            log.warn("Failed to cache URL mapping for {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Retrieve a cached long URL by short code.
     *
     * @return the long URL or null if not in cache
     */
    public String getCachedUrl(String shortCode) {
        String key = URL_CACHE_PREFIX + shortCode;
        try {
            String longUrl = redisTemplate.opsForValue().get(key);
            if (longUrl != null) {
                log.debug("Cache HIT for: {}", shortCode);
            } else {
                log.debug("Cache MISS for: {}", shortCode);
            }
            return longUrl;
        } catch (Exception e) {
            log.warn("Failed to read cache for {}: {}", shortCode, e.getMessage());
            return null;
        }
    }

    /**
     * Evict a cached URL mapping.
     */
    public void evictUrl(String shortCode) {
        String key = URL_CACHE_PREFIX + shortCode;
        try {
            redisTemplate.delete(key);
            log.debug("Evicted cache for: {}", shortCode);
        } catch (Exception e) {
            log.warn("Failed to evict cache for {}: {}", shortCode, e.getMessage());
        }
    }
}
