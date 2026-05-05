# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Resume Service — Spring Boot 3.2.3 backend for an AI-powered mock interview and resume diagnosis system. Java 21, MyBatis-Plus 3.5.7, MySQL, Redis, RabbitMQ, JWT auth.

## Build & Run

```bash
mvn spring-boot:run              # Start dev server (port 8080)
mvn compile                      # Quick compile check
mvn clean install                # Full build
mvn package -DskipTests          # Package as JAR
mvn test                         # Run all tests
mvn test -Dtest=ClassName        # Run single test class
```

## Configuration

- **Main config**: `src/main/resources/application.yml` — DB, Redis, RabbitMQ, JWT, AI providers, token limits
- **Dev overrides**: `src/main/resources/application-dev.yml` — dev-specific credentials and AI settings
- **Default profile**: `dev`
- **Database schema**: `db/schema.sql` (14 tables + seed data)
- **AI mode toggle**: `app.ai.mode` (resume diagnosis) and `app.interview.mode` (mock interview) — set to `mock` or `real`

## Architecture

### Layered Structure

```
com.airesume.server/
├── controller/          # REST endpoints (10 controllers)
├── service/             # Business interfaces
│   └── impl/            # Service implementations
├── entity/              # MyBatis-Plus entities (extends BaseEntity with snowflake ID, createTime, updateTime, isDeleted)
├── mapper/              # MyBatis-Plus mappers
├── dto/                 # Request/response DTOs (organized by domain: admin/, auth/, interview/, membership/, resume/, etc.)
├── common/              # Constants, Result<T> wrapper, ResultCode enum, BusinessException, utilities
├── config/              # Spring @Configuration classes
├── infrastructure/      # Security: JwtAuthenticationFilter, JwtUtil, JwtProperties
├── mock/                # Mock implementations (MockDiagnosisResultGenerator, MockInterviewService)
├── mq/                  # RabbitMQ: ResumeDiagnosisProducer, ResumeDiagnosisConsumer, message DTOs
├── repository/          # JPA repositories (used alongside MyBatis-Plus)
└── util/                # TokenEstimator, AiInputCompressor
```

### Key Architectural Patterns

**Dual AI Service Pattern**: For each AI feature (resume, interview), two implementations exist behind one interface:
- `InterviewAiServiceImpl` / `MockInterviewAiServiceImpl` → `InterviewAiService`
- `ResumeAiServiceImpl` / `MockResumeAiServiceImpl` → `ResumeAiService`

Selected via `@ConditionalOnProperty` on `app.interview.mode` / `app.ai.mode`.

**Runtime AI Config Resolution**: `resolveRuntimeConfig()` in AI service impls follows a 3-tier fallback: DB `sys_ai_engine_config` (active) → `application.yml` properties → environment variables.

**Async Resume Diagnosis Pipeline**: Upload → RabbitMQ message → `ResumeDiagnosisConsumer` (PDF text extraction via PDFBox → AI diagnosis → result enhancement → notification). On failure, quota is refunded.

**SSE Streaming for Interviews**: `InterviewController` uses `ResponseBodyEmitter` backed by a dedicated thread. `WebClient` (WebFlux) streams AI responses as Reactor `Flux`, forwarded as SSE events.

**Token Budget Management**: `TokenEstimator` (char-type estimation) → `AiInputCompressor` (text compression) → `InterviewContextCompressor` (conversation summarization). Limits configured in `AiTokenLimitConfig`.

**Graceful Fallback**: `InterviewAiServiceImpl.shouldFallbackToLocalMock()` falls back to `MockInterviewService` on infrastructure errors (network, timeout, missing API key) but not business logic errors.

### API Conventions

- All responses: `Result<T>` → `{"code": 200, "message": "success", "data": {...}}`
- Pagination: `PageResult<T>` with `pageNum`/`pageSize`
- Auth: JWT Bearer token, `JwtAuthenticationFilter` validates per request
- Security: `/api/auth/**` public, `/api/admin/**` requires `ROLE_ADMIN`, other `/api/**` require auth

### Prompt Management Rules

- Same job role + difficulty level can only have one active prompt. Enabling a new one auto-disables others.
- Same business type (interview/resume) can only have one active AI engine config.

### Admin API Key Security

API keys in DB are masked when returned to frontend (e.g., `sk-****abcd`). Backend validates masked values are never written back.

### Physical vs Logical Delete

Admin CRUD (prompts, job roles, AI engines) uses physical deletion. Business entities use MyBatis-Plus logical delete (`isDeleted` field via `BaseEntity`).

## Dependencies

Requires running services: MySQL 8.0+ (port 3306), Redis 6.0+ (port 6379), RabbitMQ 3.8+ (port 5672). PDF export requires Chrome on the server (path configured in `app.pdf.chrome-path`).
