package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenRequest {

    @NotBlank(message = "URL is required")
    @URL(message = "Must be a valid URL")
    private String longUrl;

    /**
     * Optional custom alias (e.g., "my-resume").
     * If null, a random short code will be generated.
     */
    private String customAlias;

    /**
     * Optional TTL in days. Defaults to app.default-ttl-days.
     */
    private Integer ttlDays;
}
