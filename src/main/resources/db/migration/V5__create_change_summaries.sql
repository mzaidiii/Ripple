CREATE table change_summaries(
    id      UUID PRIMARY KEY ,
    job_id  UUID references analysis_jobs(id) unique NOT NULL ,
    summary TEXT NOT NULL ,
    model VARCHAR(100) NOT NULL ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);