package com.urlshortener.service;

import com.urlshortener.model.ClickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes click events to Kafka for async analytics processing.
 *
 * This decouples the hot redirect path from analytics writes,
 * ensuring redirect latency stays low even under heavy load.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProducer {

    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;

    @Value("${app.kafka.click-topic}")
    private String clickTopic;

    /**
     * Publish a click event asynchronously.
     * Uses the short code as the Kafka key for partition affinity
     * (all clicks for the same URL go to the same partition).
     */
    public void publishClickEvent(String shortCode, String ipAddress, String userAgent, String referrer) {
        ClickEvent event = ClickEvent.builder()
                .shortCode(shortCode)
                .clickedAt(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referrer(referrer)
                .build();

        CompletableFuture<SendResult<String, ClickEvent>> future =
                kafkaTemplate.send(clickTopic, shortCode, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish click event for {}: {}", shortCode, ex.getMessage());
            } else {
                log.debug("Click event published for {} to partition {}",
                        shortCode, result.getRecordMetadata().partition());
            }
        });
    }
}
