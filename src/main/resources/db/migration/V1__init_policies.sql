CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS policies (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255)                NOT NULL,
    content     TEXT                        NOT NULL,
    category    VARCHAR(100)                NOT NULL DEFAULT 'GENERAL',
    status      VARCHAR(50)                 NOT NULL DEFAULT 'ACTIVE',
    effective_date TIMESTAMP,
    created_at  TIMESTAMP                   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP                   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_policies_category ON policies (category);
CREATE INDEX IF NOT EXISTS idx_policies_status   ON policies (status);
