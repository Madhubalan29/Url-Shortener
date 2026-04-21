package com.urlshortener.analytics.model;

import lombok.*;
import java.time.Instant;

/**
 * Kafka click event DTO — must match the producer's ClickEvent class.
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
