package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenResponse {

    private String shortCode;
    private String shortUrl;
    private String longUrl;
    private Instant createdAt;
    private Instant expiresAt;
    private Long clickCount;
}
