package com.complianceiq.v2.kafka;

import com.complianceiq.v2.config.KafkaTopicConfig;
import com.complianceiq.v2.service.PdfProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class PdfIngestionConsumer {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionConsumer.class);

    private final PdfProcessingService pdfProcessingService;

    public PdfIngestionConsumer(PdfProcessingService pdfProcessingService) {
        this.pdfProcessingService = pdfProcessingService;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.PDF_INGESTION_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(PdfUploadEvent event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Consuming [{}] from partition={} offset={} uploadedAt={}",
                event.fileName(), partition, offset, event.uploadedAt());
        pdfProcessingService.processAndIngest(event);
    }
}
