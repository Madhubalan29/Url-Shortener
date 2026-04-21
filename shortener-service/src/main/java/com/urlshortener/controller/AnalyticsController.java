package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Click analytics for shortened URLs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get full click analytics for a short code.
     */
    @GetMapping("/{shortCode}")
    @Operation(summary = "Get analytics", description = "Get click analytics breakdown for a shortened URL")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        AnalyticsResponse response = analyticsService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }
}
