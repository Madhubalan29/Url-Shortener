package com.urlshortener.model;

import lombok.*;
import java.time.Instant;

/**
 * DTO for Kafka click events.
 * Serialized as JSON and published to the "url-clicks" topic.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    private String shortCode;
    private Instant clickedAt;
    private String ipAddress;
    private String userAgent;
    private String referrer;
}
