ALTER TABLE analysis_jobs
     ADD started_at TIMESTAMPTZ ,
     ADD next_run_at    TIMESTAMPTZ  NOT NULL DEFAULT now();