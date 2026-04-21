package com.urlshortener.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.click-topic}")
    private String clickTopic;

    @Bean
    public NewTopic clickEventsTopic() {
        return TopicBuilder.name(clickTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
