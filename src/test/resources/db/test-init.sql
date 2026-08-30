-- Mirrors student360-infra/infra/init-db/03-audit.sql (the audit table is owned by infra, never by
-- a service migration). The network schema itself is created and migrated by Flyway.
CREATE SCHEMA audit;
CREATE TABLE audit.audit_record (
    id                   BIGSERIAL PRIMARY KEY,
    occurred_at          TIMESTAMPTZ  NOT NULL,
    request_id           TEXT         NOT NULL,
    trace_id             TEXT,
    service_name         TEXT         NOT NULL,
    record_type          TEXT         NOT NULL,
    action               TEXT         NOT NULL,
    actor_id             UUID,
    actor_roles          TEXT[],
    subject_type         TEXT,
    subject_id           TEXT,
    authorization_basis  TEXT,
    outcome              TEXT         NOT NULL,
    source_ip            TEXT,
    details              JSONB
);
