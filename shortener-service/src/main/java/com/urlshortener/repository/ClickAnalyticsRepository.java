package com.urlshortener.repository;

import com.urlshortener.model.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    long countByShortCode(String shortCode);

    List<ClickAnalytics> findByShortCodeAndClickedAtBetweenOrderByClickedAtDesc(
            String shortCode, Instant start, Instant end);

    @Query("SELECT DATE(c.clickedAt) as date, COUNT(c) as count " +
           "FROM ClickAnalytics c WHERE c.shortCode = :shortCode " +
           "GROUP BY DATE(c.clickedAt) ORDER BY DATE(c.clickedAt) DESC")
    List<Object[]> countClicksByDate(@Param("shortCode") String shortCode);

    @Query("SELECT c.country, COUNT(c) FROM ClickAnalytics c " +
           "WHERE c.shortCode = :shortCode AND c.country IS NOT NULL " +
           "GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countClicksByCountry(@Param("shortCode") String shortCode);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickAnalytics c " +
           "WHERE c.shortCode = :shortCode AND c.deviceType IS NOT NULL " +
           "GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> countClicksByDevice(@Param("shortCode") String shortCode);

    @Query("SELECT c.referrer, COUNT(c) FROM ClickAnalytics c " +
           "WHERE c.shortCode = :shortCode AND c.referrer IS NOT NULL " +
           "GROUP BY c.referrer ORDER BY COUNT(c) DESC")
    List<Object[]> countClicksByReferrer(@Param("shortCode") String shortCode);
}
