-- network schema: NOT where the graph lives (that is Neo4j) — only what student360-common's
-- audit writer and outbox publisher need, exactly like every other service's schema holds those
-- two structures. Owned by network_user, migrated only by network-service.

CREATE TABLE network.outbox_event (
    id              UUID PRIMARY KEY,
    event_type      TEXT        NOT NULL,
    aggregate_type  TEXT        NOT NULL,
    aggregate_id    TEXT        NOT NULL,
    payload         JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_event_unpublished ON network.outbox_event (created_at) WHERE published_at IS NULL;
