# Database

Ripple uses PostgreSQL with the pgvector extension. Schema changes are applied by Flyway on startup, and Hibernate runs with `ddl-auto=validate`, so every entity is checked against the real table when the application boots.

Migrations live in `src/main/resources/db/migration`.

| Migration | Purpose |
|---|---|
| V1 | `webhook_deliveries` table |
| V2 | `analysis_jobs` table |
| V3 | `started_at` and `next_run_at` columns on `analysis_jobs` |
| V4 | partial index on pending jobs |
| V5 | `change_summaries` table |
| V6 | pgvector extension and the `code_chunks` table |

## webhook_deliveries

An append-only record of what GitHub sent.

| Column | Notes |
|---|---|
| delivery_id | unique, taken from the `X-GitHub-Delivery` header |
| event type, repository, ref, before and after SHAs | extracted from the payload |
| payload | the full request body as `jsonb` |
| status | `RECEIVED` or `REJECTED` |
| timestamps | `timestamptz` |

## analysis_jobs

The working queue.

| Column | Notes |
|---|---|
| id | UUID primary key |
| delivery_id | unique foreign key to `webhook_deliveries`, so one push produces at most one job |
| repo_full_name, before_sha, after_sha | copied from the delivery so a worker reads one narrow row |
| status | `PENDING`, `RUNNING`, `DONE` or `FAILED`, enforced by a check constraint |
| attempts | incremented each time a worker claims the job |
| last_error | text |
| started_at | set when a worker claims the job |
| next_run_at | the job is not eligible before this time, used for retry delays |
| created_at, finished_at | `timestamptz` |

The partial index from V4 covers only `PENDING` rows, so the claim query stays fast as finished jobs accumulate.

## change_summaries

The output of the summary pipeline.

| Column | Notes |
|---|---|
| id | UUID primary key |
| job_id | unique foreign key to `analysis_jobs`, so a job has at most one summary |
| summary | text |
| model | the model that wrote the summary |
| created_at | `timestamptz` |

## code_chunks

The searchable index of a repository's code.

| Column | Notes |
|---|---|
| id | UUID primary key |
| repo_full_name | the repository the chunk belongs to |
| file_path | path of the file inside the repository |
| start_line, end_line | line range of the chunk, 1-based, used for citations |
| content | the chunk text |
| content_hash | SHA-256 of the text, used to skip chunks that are already embedded |
| commit_sha | the commit the chunk was embedded at |
| embedding | `vector(768)` |
| created_at | `timestamptz` |

An index on `(repo_full_name, file_path)` supports deleting and replacing the chunks of one file. Searches always filter by `repo_full_name` first and then order by cosine distance (`<=>`), so results never cross repositories. A vector index will be added once there is enough data to need one.

The vector size has to match the embedding model's output size. It is set with the `gemini.embedding-dimensions` property and the column definition together, so changing one means changing the other.

## Why separate tables

- Deliveries are an immutable record of what arrived. Jobs are small working state that workers update constantly. Keeping a large `jsonb` payload on a row that changes every few seconds would be wasteful, because PostgreSQL writes a new version of the row on every update.
- Summaries are written once and meant to be kept, while jobs could be pruned later. A job in `PENDING`, `RUNNING` or `FAILED` never has a summary, so a column on the job table would sit empty for many rows.
- If one job later produces several summaries, for example one per file or one per model, the unique constraint on `job_id` can be dropped without touching the job table.
- Code chunks belong to a repository and a commit, not to a job, so they have their own table.