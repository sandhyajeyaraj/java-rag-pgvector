package com.complianceiq.v2.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String PDF_INGESTION_TOPIC = "pdf-ingestion";

    @Bean
    public NewTopic pdfIngestionTopic() {
        return TopicBuilder.name(PDF_INGESTION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
