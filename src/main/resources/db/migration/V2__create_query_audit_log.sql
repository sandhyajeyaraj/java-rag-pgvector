CREATE TABLE IF NOT EXISTS query_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    question        TEXT            NOT NULL,
    llm_provider    VARCHAR(50),
    risk_level      VARCHAR(20),
    compliance_category VARCHAR(100),
    confidence      DOUBLE PRECISION,
    cached          BOOLEAN         NOT NULL DEFAULT FALSE,
    success         BOOLEAN         NOT NULL,
    duration_ms     BIGINT,
    error_message   TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_created_at ON query_audit_log (created_at DESC);
CREATE INDEX idx_audit_llm_provider ON query_audit_log (llm_provider);
