CREATE TABLE analysis_jobs (
       id             UUID PRIMARY KEY,
       delivery_id    VARCHAR(255) NOT NULL UNIQUE
                       REFERENCES webhook_deliveries (delivery_id),
       repo_full_name VARCHAR(255) NOT NULL,
       before_sha     VARCHAR(255) NOT NULL,
       after_sha      VARCHAR(255) NOT NULL,
       status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
       attempts       INT          NOT NULL DEFAULT 0,
       last_error     TEXT,
       created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
       finished_at    TIMESTAMPTZ,
       CONSTRAINT chk_analysis_jobs_status
       CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED'))
);