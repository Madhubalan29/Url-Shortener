package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for querying click analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UrlMappingRepository urlMappingRepository;

    /**
     * Get full analytics for a short code.
     */
    public AnalyticsResponse getAnalytics(String shortCode) {
        long totalClicks = clickAnalyticsRepository.countByShortCode(shortCode);

        Map<String, Long> clicksByDate = toMap(clickAnalyticsRepository.countClicksByDate(shortCode));
        Map<String, Long> clicksByCountry = toMap(clickAnalyticsRepository.countClicksByCountry(shortCode));
        Map<String, Long> clicksByDevice = toMap(clickAnalyticsRepository.countClicksByDevice(shortCode));
        Map<String, Long> clicksByReferrer = toMap(clickAnalyticsRepository.countClicksByReferrer(shortCode));

        return AnalyticsResponse.builder()
                .shortCode(shortCode)
                .totalClicks(totalClicks)
                .clicksByDate(clicksByDate)
                .clicksByCountry(clicksByCountry)
                .clicksByDevice(clicksByDevice)
                .clicksByReferrer(clicksByReferrer)
                .build();
    }

    private Map<String, Long> toMap(List<Object[]> results) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            String key = row[0] != null ? row[0].toString() : "unknown";
            Long value = ((Number) row[1]).longValue();
            map.put(key, value);
        }
        return map;
    }
}
