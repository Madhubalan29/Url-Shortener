package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsResponse {

    private String shortCode;
    private Long totalClicks;
    private Map<String, Long> clicksByDate;
    private Map<String, Long> clicksByCountry;
    private Map<String, Long> clicksByDevice;
    private Map<String, Long> clicksByReferrer;
}
