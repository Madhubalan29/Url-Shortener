package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.model.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    @Modifying
    @Query(value = "UPDATE url_mappings SET click_count = click_count + :count WHERE short_code = :shortCode",
           nativeQuery = true)
    void incrementClickCount(@Param("shortCode") String shortCode, @Param("count") long count);
}
