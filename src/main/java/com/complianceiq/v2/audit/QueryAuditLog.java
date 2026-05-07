package com.complianceiq.v2.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "query_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "llm_provider", length = 50)
    private String llmProvider;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "compliance_category", length = 100)
    private String complianceCategory;

    private Double confidence;

    @Column(nullable = false)
    private boolean cached;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
    }
}
