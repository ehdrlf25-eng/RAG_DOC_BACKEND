CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    event_id       UUID        NOT NULL UNIQUE,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   BIGINT      NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB       NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMPTZ,
    retry_count    INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (status, created_at)
    WHERE status = 'PENDING';
