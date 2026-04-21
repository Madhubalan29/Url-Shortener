package com.urlshortener.controller;

import com.urlshortener.model.ApiKey;
import com.urlshortener.repository.ApiKeyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API key management")
public class AuthController {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Create a new API key.
     * This endpoint is public (no auth required) for bootstrapping.
     */
    @PostMapping("/keys")
    @Operation(summary = "Create API key", description = "Generate a new API key for accessing the URL shortener")
    public ResponseEntity<?> createApiKey(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "Default");

        // Generate a secure random API key
        String rawKey = "usk_" + UUID.randomUUID().toString().replace("-", "");

        ApiKey apiKey = ApiKey.builder()
                .apiKey(rawKey)
                .name(name)
                .rateLimit(100)
                .createdAt(Instant.now())
                .isActive(true)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Created API key '{}' for: {}", name, apiKey.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", apiKey.getId(),
                "apiKey", rawKey,
                "name", apiKey.getName(),
                "rateLimit", apiKey.getRateLimit(),
                "message", "Store this API key securely. It will not be shown again."
        ));
    }

    /**
     * Revoke an API key.
     */
    @DeleteMapping("/keys/{id}")
    @Operation(summary = "Revoke API key", description = "Deactivate an API key")
    public ResponseEntity<?> revokeApiKey(@PathVariable UUID id) {
        return apiKeyRepository.findById(id)
                .map(apiKey -> {
                    apiKey.setIsActive(false);
                    apiKeyRepository.save(apiKey);
                    log.info("Revoked API key: {}", id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
