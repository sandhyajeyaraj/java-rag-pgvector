package com.complianceiq.v2.audit;

import com.complianceiq.v2.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final QueryAuditLogRepository repository;

    @Async("aiThreadPool")
    public void record(String question, QueryResponse response, boolean cached, long durationMs) {
        try {
            QueryAuditLog entry = QueryAuditLog.builder()
                    .question(question)
                    .llmProvider(response.getLlmProvider())
                    .riskLevel(response.getRiskLevel())
                    .complianceCategory(response.getComplianceCategory())
                    .confidence(response.getConfidence())
                    .cached(cached)
                    .success(true)
                    .durationMs(durationMs)
                    .build();

            repository.save(entry);
            log.info("Audit saved — provider={} cached={} duration={}ms", response.getLlmProvider(), cached, durationMs);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    @Async("aiThreadPool")
    public void recordError(String question, long durationMs, String errorMessage) {
        try {
            QueryAuditLog entry = QueryAuditLog.builder()
                    .question(question)
                    .cached(false)
                    .success(false)
                    .durationMs(durationMs)
                    .errorMessage(errorMessage)
                    .build();

            repository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save error audit log", e);
        }
    }
}
