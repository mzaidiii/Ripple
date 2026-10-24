CREATE INDEX idx_analysis_jobs_pending
    ON analysis_jobs (next_run_at)
    WHERE status = 'PENDING';