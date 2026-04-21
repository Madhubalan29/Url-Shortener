package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Token bucket rate limiter using Redis.
 *
 * Uses a Lua script for atomic check-and-decrement, preventing race conditions
 * in distributed environments. Each API key gets its own bucket.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rl:";

    @Value("${app.rate-limit.requests}")
    private int defaultMaxRequests;

    @Value("${app.rate-limit.window-seconds}")
    private int windowSeconds;

    /**
     * Lua script for atomic rate limiting.
     * Returns 1 if the request is allowed, 0 if rate limited.
     *
     * Logic:
     * 1. If key doesn't exist, create it with max requests and set TTL
     * 2. If remaining > 0, decrement and allow
     * 3. Otherwise, reject
     */
    private static final String RATE_LIMIT_SCRIPT =
        "local key = KEYS[1] " +
        "local max_requests = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local current = redis.call('GET', key) " +
        "if current == false then " +
        "  redis.call('SET', key, max_requests - 1, 'EX', window) " +
        "  return 1 " +
        "end " +
        "if tonumber(current) > 0 then " +
        "  redis.call('DECR', key) " +
        "  return 1 " +
        "end " +
        "return 0";

    /**
     * Check if a request is allowed under the rate limit.
     *
     * @param apiKey      the API key identifier
     * @param maxRequests the maximum number of requests in the window (from ApiKey entity)
     * @return true if the request is allowed, false if rate limited
     */
    public boolean isAllowed(String apiKey, int maxRequests) {
        String key = RATE_LIMIT_PREFIX + apiKey;

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(maxRequests),
                String.valueOf(windowSeconds)
            );
            boolean allowed = result != null && result == 1L;
            if (!allowed) {
                log.info("Rate limit exceeded for API key: {}", maskApiKey(apiKey));
            }
            return allowed;
        } catch (Exception e) {
            log.warn("Rate limiter error for {}: {} — allowing request (fail-open)", maskApiKey(apiKey), e.getMessage());
            // Fail-open: if Redis is down, allow the request
            return true;
        }
    }

    /**
     * Check if a request is allowed using the default rate limit.
     */
    public boolean isAllowed(String apiKey) {
        return isAllowed(apiKey, defaultMaxRequests);
    }

    /**
     * Get remaining requests for an API key.
     */
    public long getRemainingRequests(String apiKey) {
        String key = RATE_LIMIT_PREFIX + apiKey;
        try {
            String remaining = redisTemplate.opsForValue().get(key);
            return remaining != null ? Long.parseLong(remaining) : defaultMaxRequests;
        } catch (Exception e) {
            return defaultMaxRequests;
        }
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) return "***";
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
