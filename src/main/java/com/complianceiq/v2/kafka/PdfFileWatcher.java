package com.complianceiq.v2.kafka;

import com.complianceiq.v2.config.KafkaTopicConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches the uploads directory for new or updated PDF files and publishes
 * a PdfUploadEvent to Kafka for each one. Includes a 1-second debounce to
 * suppress duplicate OS-level events fired for a single file write.
 */
@Component
public class PdfFileWatcher {

    private static final Logger log = LoggerFactory.getLogger(PdfFileWatcher.class);
    private static final long DEBOUNCE_MS = 1000;

    @Value("${pdf.upload.directory:./uploadFile}")
    private String uploadDirectory;

    private final KafkaTemplate<String, PdfUploadEvent> kafkaTemplate;
    private final Map<String, Long> lastEventTime = new ConcurrentHashMap<>();

    public PdfFileWatcher(KafkaTemplate<String, PdfUploadEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void startWatching() throws IOException {
        Path uploadPath = Path.of(uploadDirectory);
        Files.createDirectories(uploadPath);

        Thread watcher = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                uploadPath.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                log.info("Watching for PDF uploads in: {}", uploadPath.toAbsolutePath());

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = uploadPath.resolve((Path) event.context());
                        String fileName = changed.getFileName().toString();

                        if (!fileName.toLowerCase().endsWith(".pdf")) continue;

                        // debounce — skip if same file fired an event less than 1s ago
                        long now = System.currentTimeMillis();
                        Long last = lastEventTime.put(fileName, now);
                        if (last != null && now - last < DEBOUNCE_MS) continue;

                        PdfUploadEvent uploadEvent = new PdfUploadEvent(
                                fileName,
                                changed.toAbsolutePath().toString(),
                                Instant.now()
                        );

                        kafkaTemplate.send(KafkaTopicConfig.PDF_INGESTION_TOPIC, fileName, uploadEvent);
                        log.info("Detected PDF [{}], published to Kafka (event: {})",
                                fileName, event.kind().name());
                    }

                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.error("File watcher failed", e);
            }
        }, "pdf-file-watcher");

        watcher.setDaemon(true);
        watcher.start();
    }
}
