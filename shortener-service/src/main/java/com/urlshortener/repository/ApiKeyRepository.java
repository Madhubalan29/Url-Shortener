package com.urlshortener.repository;

import com.urlshortener.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByApiKeyAndIsActiveTrue(String apiKey);

    boolean existsByApiKey(String apiKey);
}
