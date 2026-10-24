# Ripple

Ripple is a backend service that receives GitHub push events and turns each one into a queued analysis job. The long-term goal is a GitHub App that summarises code changes, reports their impact with evidence, and answers questions about a single project. This repository currently contains the ingestion layer: verified webhook intake, deduplication, durable storage, and a job queue backed by PostgreSQL.

## Status

Under active development. The webhook ingestion pipeline and job creation are implemented and tested. The worker, GitHub API integration, summarisation, and chatbot are planned (see the roadmap below).

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
```

GitHub expects a reply within a few seconds, so the endpoint only records what happened and queues the work. Anything slow is meant to run later, outside the request.

Design decisions in the current code:

- Signatures are checked against the raw request body with a constant-time comparison before anything is parsed or stored.
- Each delivery is identified by GitHub's delivery id, which is unique in both tables. A redelivery is detected and acknowledged without creating a second row.
- The delivery row and the job row are written in one transaction, so a push is never recorded without a job.
- Two identical deliveries arriving at the same moment can both pass the existence check. The unique constraint rejects the second insert, and the controller handles that case as a duplicate.
- Secrets are read from environment variables and are never committed.

## Tech stack

- Java and Spring Boot (Web, Data JPA, Validation, Actuator)
- PostgreSQL
- Flyway for schema migrations, with Hibernate set to `validate` so the entities are checked against the real schema on startup
- Lombok
- Maven (wrapper included)

## Database schema

Migrations live in `src/main/resources/db/migration`.

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
| attempts | retry counter |
| last_error | text |
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

Flyway applies the migrations on startup. The server listens on port 8080.

### Connect a GitHub repository

1. Start a tunnel: `ngrok http 8080`
2. In the repository settings, add a webhook with the payload URL `https://<your-tunnel-host>/api/webhooks/github`.
3. Set the content type to `application/json`.
4. Enter the same value you used for `GITHUB_WEBHOOK_SECRET` as the secret.
5. Select the push event only.

Push a commit. The application log shows the push, and one row appears in each table:

```sql
SELECT delivery_id, status FROM webhook_deliveries ORDER BY id DESC LIMIT 1;
SELECT delivery_id, status, attempts FROM analysis_jobs ORDER BY created_at DESC LIMIT 1;
```

## Verified behaviour

The following cases were exercised manually against a real repository:

- A push creates one delivery row and one `PENDING` job.
- Redelivering the same push from GitHub is logged as a duplicate and adds no rows.
- A request with a missing or invalid signature returns 401 and stores nothing.
- A failure after the delivery is saved rolls back both inserts, and a later redelivery succeeds.

Automated integration tests are planned (see the roadmap).

## Project layout

```
org.personal.ripple
  controller    HTTP endpoints
  services      webhook handling and job creation
  entity        JPA entities and enums
  repository    Spring Data repositories
  security      signature verification
```

## Roadmap

1. Worker that claims `PENDING` jobs safely with `SELECT ... FOR UPDATE SKIP LOCKED`, with retries, backoff, and recovery of jobs stuck in `RUNNING`.
2. Fetch the real change from GitHub's compare API and move to a GitHub App with installation tokens.
3. LLM-generated change summaries, posted back to the pull request or commit.
4. Code indexing with pgvector and a project-scoped chatbot with citations.
5. Impact analysis for Java code using a dependency graph built from the source.
6. Hardening: Kafka in place of the job table, Redis, Testcontainers tests, load tests, metrics.