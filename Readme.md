# Ripple

Ripple is a backend service that receives GitHub push events and turns each one into a queued analysis job. The long-term goal is a GitHub App that summarises code changes, reports their impact with evidence, and answers questions about a single project. This repository currently contains the ingestion layer and the first version of the job worker: verified webhook intake, deduplication, durable storage, a PostgreSQL-backed job queue, and a scheduled worker that claims jobs safely.

## Status

Under active development. Implemented and tested: webhook ingestion, job creation, and a worker that claims pending jobs and marks them done. The processing step inside the worker is a placeholder for now. Retries, recovery of stuck jobs, GitHub API integration, summarisation, and the chatbot are planned (see the roadmap).

## How it works

```
GitHub push event
      |
      v
POST /api/webhooks/github
      |
      |-- verify HMAC-SHA256 signature  --> 401 if invalid
      |-- ignore events other than "push"
      |
      v
single database transaction
      |-- reject duplicate delivery ids
      |-- store the raw delivery
      |-- create a PENDING analysis job
      v
200 OK returned to GitHub

                    (later, independently)
scheduled worker, every 5 seconds
      |
      v
claim one job in a short transaction
      |-- SELECT ... FOR UPDATE SKIP LOCKED
      |-- status = RUNNING, started_at = now, attempts + 1
      v
do the work (placeholder)
      v
mark the job DONE in a second short transaction
```

GitHub expects a reply within a few seconds, so the endpoint only records what happened and queues the work. Anything slow runs in the worker, outside the request.

Design decisions in the current code:

- Signatures are checked against the raw request body with a constant-time comparison before anything is parsed or stored.
- Each delivery is identified by GitHub's delivery id, which is unique in both tables. A redelivery is detected and acknowledged without creating a second row.
- The delivery row and the job row are written in one transaction, so a push is never recorded without a job.
- Two identical deliveries arriving at the same moment can both pass the existence check. The unique constraint rejects the second insert, and the controller handles that case as a duplicate.
- Workers claim jobs with `FOR UPDATE SKIP LOCKED`, so a row locked by one worker is skipped by the others instead of being taken twice.
- Claiming and finishing are separate short transactions. No database lock is held while a job is being processed.
- A partial index on pending jobs keeps the claim query fast as finished jobs accumulate.
- Secrets are read from environment variables and are never committed.

## Tech stack

- Java and Spring Boot (Web, Data JPA, Validation, Actuator, scheduling)
- PostgreSQL
- Flyway for schema migrations, with Hibernate set to `validate` so the entities are checked against the real schema on startup
- Lombok
- Maven (wrapper included)

## Database schema

Migrations live in `src/main/resources/db/migration`.

| Migration | Purpose |
|---|---|
| V1 | `webhook_deliveries` table |
| V2 | `analysis_jobs` table |
| V3 | `started_at` and `next_run_at` columns on `analysis_jobs` |
| V4 | partial index on pending jobs |

`webhook_deliveries` is an append-only record of what GitHub sent.

| Column | Notes |
|---|---|
| delivery_id | unique, taken from the `X-GitHub-Delivery` header |
| event type, repository, ref, before and after SHAs | extracted from the payload |
| payload | the full request body as `jsonb` |
| status | `RECEIVED` or `REJECTED` |
| timestamps | `timestamptz` |

`analysis_jobs` is the working queue.

| Column | Notes |
|---|---|
| id | UUID primary key |
| delivery_id | unique, foreign key to `webhook_deliveries`, so one push produces at most one job |
| repo_full_name, before_sha, after_sha | copied from the delivery so a worker reads one narrow row |
| status | `PENDING`, `RUNNING`, `DONE` or `FAILED`, enforced by a check constraint |
| attempts | incremented each time a worker claims the job |
| last_error | text |
| started_at | set when a worker claims the job |
| next_run_at | the job is not eligible before this time, used for retry delays |
| created_at, finished_at | `timestamptz` |

## Getting started

### Prerequisites

- A JDK compatible with the version declared in `pom.xml`
- PostgreSQL running locally, with an empty database for Ripple
- A tunnelling tool such as ngrok for receiving webhooks on your machine

### Configuration

The database connection settings are in `src/main/resources/application.properties`. Two values are supplied through environment variables and have no defaults:

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | password for the PostgreSQL user |
| `GITHUB_WEBHOOK_SECRET` | shared secret used to verify webhook signatures |

Set both in your shell or run configuration before starting the application.

### Run

```bash
./mvnw spring-boot:run
```

Flyway applies the migrations on startup. The server listens on port 8080 and the worker starts polling immediately.

### Connect a GitHub repository

1. Start a tunnel: `ngrok http 8080`
2. In the repository settings, add a webhook with the payload URL `https://<your-tunnel-host>/api/webhooks/github`.
3. Set the content type to `application/json`.
4. Enter the same value you used for `GITHUB_WEBHOOK_SECRET` as the secret.
5. Select the push event only.

Push a commit. The application log shows the push, then the worker claiming the job within a few seconds. To inspect the result:

```sql
SELECT delivery_id, status FROM webhook_deliveries;
SELECT repo_full_name, status, attempts, started_at, finished_at
FROM analysis_jobs ORDER BY created_at DESC LIMIT 5;
```

## Verified behaviour

The following cases were exercised manually against a real repository:

- A push creates one delivery row and one `PENDING` job.
- Redelivering the same push from GitHub is logged as a duplicate and adds no rows.
- A request with a missing or invalid signature returns 401 and stores nothing.
- A failure after the delivery is saved rolls back both inserts, and a later redelivery succeeds.
- The worker claims a pending job, sets `RUNNING`, `started_at` and `attempts = 1`, and then marks it `DONE` with `finished_at` set.

Testing with several workers at once, and automated integration tests, are planned (see the roadmap).

## Project layout

```
org.personal.ripple
  controller    HTTP endpoints
  services      webhook handling, job claiming, scheduled worker
  entity        JPA entities and enums
  repository    Spring Data repositories
  security      signature verification
```

## Roadmap

1. Failure handling: retries with increasing delay, a limit of three attempts, and a `FAILED` state that keeps the error for inspection.
2. Recovery of jobs left in `RUNNING` after a crash, based on `started_at`.
3. Fetch the real change from GitHub's compare API and move to a GitHub App with installation tokens.
4. LLM-generated change summaries, posted back to the pull request or commit.
5. Code indexing with pgvector and a project-scoped chatbot with citations.
6. Impact analysis for Java code using a dependency graph built from the source.
7. Hardening: Kafka in place of the job table, Redis, Testcontainers tests including concurrent workers, load tests, metrics.