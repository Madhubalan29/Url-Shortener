package com.urlshortener.analytics.consumer;

import com.urlshortener.analytics.model.ClickAnalytics;
import com.urlshortener.analytics.model.ClickEvent;
import com.urlshortener.analytics.repository.ClickAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka consumer that processes click events and persists analytics.
 *
 * Design decisions:
 * - Batch processing: accumulates events and writes in batches for efficiency
 * - Device detection: parses User-Agent to extract device type
 * - Click count update: also increments the denormalized click_count on url_mappings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClickEventConsumer {

    private final ClickAnalyticsRepository clickAnalyticsRepository;

    @KafkaListener(
        topics = "${app.kafka.click-topic}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeClickEvent(ClickEvent event) {
        try {
            // Parse device type from User-Agent
            String deviceType = parseDeviceType(event.getUserAgent());

            // Build analytics record
            ClickAnalytics analytics = ClickAnalytics.builder()
                    .shortCode(event.getShortCode())
                    .clickedAt(event.getClickedAt())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .referrer(event.getReferrer())
                    .deviceType(deviceType)
                    .build();

            // Persist
            clickAnalyticsRepository.save(analytics);

            // Increment denormalized click count
            clickAnalyticsRepository.incrementClickCount(event.getShortCode(), 1);

            log.debug("Processed click event for: {} (device: {})", event.getShortCode(), deviceType);
        } catch (Exception e) {
            log.error("Failed to process click event for {}: {}", event.getShortCode(), e.getMessage(), e);
            // Don't rethrow — let Kafka commit the offset to avoid infinite retry loops.
            // In production, you'd send to a dead-letter topic.
        }
    }

    /**
     * Batch listener for higher throughput (alternative to single-event listener).
     * Uncomment this and comment out the single listener above to use batch mode.
     */
    /*
    @KafkaListener(
        topics = "${app.kafka.click-topic}",
        groupId = "${spring.kafka.consumer.group-id}",
        batch = "true"
    )
    @Transactional
    public void consumeClickEventBatch(List<ClickEvent> events) {
        List<ClickAnalytics> batch = new ArrayList<>();
        Map<String, Long> clickCounts = new HashMap<>();

        for (ClickEvent event : events) {
            String deviceType = parseDeviceType(event.getUserAgent());

            batch.add(ClickAnalytics.builder()
                    .shortCode(event.getShortCode())
                    .clickedAt(event.getClickedAt())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .referrer(event.getReferrer())
                    .deviceType(deviceType)
                    .build());

            clickCounts.merge(event.getShortCode(), 1L, Long::sum);
        }

        clickAnalyticsRepository.saveAll(batch);
        clickCounts.forEach(clickAnalyticsRepository::incrementClickCount);
        log.info("Processed batch of {} click events", events.size());
    }
    */

    // ─── Device Detection ────────────────────────────────

    /**
     * Simple User-Agent parser to detect device type.
     * For production, use a library like UADetector or Browscap.
     */
    private String parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "tablet";
        } else if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) {
            return "bot";
        } else {
            return "desktop";
        }
    }
}
