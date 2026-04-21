package com.urlshortener.repository;

import com.urlshortener.model.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCodeAndIsActiveTrue(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<UrlMapping> findByApiKeyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID apiKeyId, Pageable pageable);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + :count WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode, @Param("count") long count);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.isActive = false WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    int deactivateExpiredUrls(@Param("now") Instant now);
}
