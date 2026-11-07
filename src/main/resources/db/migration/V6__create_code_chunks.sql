CREATE EXTENSION IF NOT EXISTS vector;
CREATE table code_chunks(
    id UUID PRIMARY KEY ,
    repo_full_name VARCHAR(255) NOT NULL ,
    file_path Text NOT NULL ,
    start_line int NOT NULL ,
    end_line int NOT NULL ,
    content Text NOT NULL ,
    content_hash VARCHAR(64) NOT NULL ,
    commit_sha VARCHAR(64) NOT NULL ,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_code_chunks_repo_file ON code_chunks (repo_full_name, file_path);