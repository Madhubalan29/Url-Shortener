package com.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "click_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 10)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    @Builder.Default
    private Instant clickedAt = Instant.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", length = 2048)
    private String referrer;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "device_type", length = 20)
    private String deviceType;
}
