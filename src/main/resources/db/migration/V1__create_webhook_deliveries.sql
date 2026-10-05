CREATE TABLE webhook_deliveries (
     id UUID PRIMARY KEY,
     delivery_id   VARCHAR(255) NOT NULL UNIQUE,
     event_type    VARCHAR(255) NOT NULL,
     repo_full_name VARCHAR(255) NOT NULL,
     ref  VARCHAR(255) NOT NULL,
     before_sha  VARCHAR(255) NOT NULL,
     after_sha   VARCHAR(255) NOT NULL,
     payload  JSONB NOT NULL,
     status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED'
     CHECK (status IN ('RECEIVED', 'REJECTED')),
     received_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);