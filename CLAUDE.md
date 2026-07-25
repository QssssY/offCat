# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.



Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

------

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## Project Overview

AI Resume（AI简历诊断与模拟面试系统）— a frontend-backend separated SPA + REST API application for AI-powered resume diagnosis and mock interviews.

## Commands

### Backend (server/)

```bash
cd server
mvn clean compile        # Compile only
mvn clean package        # Full build (skip tests: -DskipTests)
mvn spring-boot:run      # Start server on :8080
mvn test                 # Run tests
```

Prerequisites: MySQL 8.0+, Redis 6.0+, RabbitMQ 3.8+ all running locally.

### Frontend (frontend/app/)

```bash
cd frontend/app
npm install              # Install deps
npm run dev              # Vite dev server on :3000 (proxies /api/* and /auth/* to :8080)
npm run build            # Production build
npm run preview          # Preview production build
```

### Database

```bash
mysql -u root -p < db/schema.sql    # Full schema (17 tables)
```

## Architecture

```
Vue 3 SPA (:3000)  --->  Spring Boot API (:8080)  --->  MySQL / Redis / RabbitMQ
                                          |
                                          +--->  AI Provider APIs (DeepSeek, Doubao, etc.)
```

### Backend (Java 21, Spring Boot 3.2.3)

Package root: `com.airesume.server`. Layering: **Controller → Service (interface) → ServiceImpl → Mapper (MyBatis-Plus)**.

Key packages:
- `controller/` — 10 REST controllers (auth, resume, interview, admin, membership, notification, growth, onboarding, debug)
- `service/` + `service/impl/` — Business logic; implementations switched via `@ConditionalOnProperty` for mock vs real AI
- `entity/` — MyBatis-Plus entities with BaseEntity (id/snowflake, createTime, updateTime, isDeleted)
- `dto/` — Request/response DTOs organized by domain (auth/, interview/, resume/, admin/, etc.)
- `mapper/` — MyBatis-Plus mapper interfaces (primary data access)
- `repository/` — Spring Data JPA repos (supplementary)
- `config/` — SecurityConfig, RabbitMQConfig, RedisConfig, MybatisPlusConfig, RestClientConfig, AsyncConfig
- `infrastructure/security/` — JWT (JwtUtil, JwtAuthenticationFilter)
- `mq/` — RabbitMQ producer/consumer for async resume diagnosis pipeline
- `common/result/` — Unified response wrapper: `Result<T>` with code/message/data
- `common/exception/` — BusinessException + GlobalExceptionHandler

All API responses use `Result.success(...)` / `Result.error(...)`. Verify method signatures match the actual `Result` class before outputting code.

### Frontend (Vue 3 + Vite + Element Plus + Pinia)

Source root: `frontend/app/src/`. Key directories:
- `api/` — Domain-specific HTTP modules (auth, interview, resume, membership, notification, growth, onboarding, admin/)
- `views/` — Page-level components (user routes + admin routes under `/admin/*`)
- `components/` — Reusable components; `resume/` has 14 components including ResumeTemplate.vue (largest), ResumeInlineRichEditor
- `template/` — Resume template system (contents, styles, industries.js, templates.js)
- `stores/` — Pinia stores (user.js, adminUser.js, theme.js)
- `router/` — Vue Router with separate auth guards for user vs admin routes
- `layouts/` — MainLayout.vue (user), AdminLayout.vue (admin)
- `utils/` — request.js (Axios with auth interceptor), auth.js, adminAuth.js

### Key Data Flows

**Resume Diagnosis (async):** Upload → Controller → TaskService → RabbitMQ producer → Consumer → PDFBox extraction → AI diagnosis → persist result to DB

**Mock Interview (streaming):** Message → Controller → InterviewService → AI generateReplyStream → ResponseBodyEmitter SSE → frontend

## Conventions

- **Chinese comments required** on all new/modified core code. Must cover: key state variables, submission/display/copy logic, Controller endpoints, Service methods, AI calls, PDF fallback logic, important fields.
- **No scope creep**: only implement what the current task explicitly requires. Do not add features beyond the stated scope.
- **Minimize risk**: prefer smallest viable change. Do not refactor unrelated code or restructure core tables unnecessarily.
- **Dual ORM**: MyBatis-Plus mappers are primary; JPA repositories are supplementary. New data access should use MyBatis-Plus.
- **Table conventions**: All tables must have `id` (BIGINT snowflake), `create_time`, `update_time`, `is_deleted`.
- **API doc sync**: When adding/modifying HTTP endpoints, update docs in `docs/api/` following `runtime/API_DOC_RULES.md`.
- **Logging**: Follow `runtime/LOG_RULES.md` — key business flows, state transitions, and exception handling must have logs.
- **Frontend**: All HTTP calls go through `api/` modules, never raw Axios in views. Element Plus is the UI library. Follow `frontend/docs/UI_STYLE_GUIDE.md`, `frontend/docs/PAGE_SKELETON_GUIDE.md`, `frontend/docs/COMPONENT_GUIDE.md`.
- **Task files**: Backend tasks in `tasks/`, frontend tasks in `frontend/tasks/`. Never mix. Update stage files after completing features.
- **Build verification required**: Backend must compile (`mvn clean compile`), frontend must build (`npm run build`), before delivery.

## Backend Coding Conventions

### Dependency Injection

- Use `@RequiredArgsConstructor` + `private final` fields for constructor injection (Lombok generates the constructor)
- `@Autowired(required = false)` only for optional dependencies (e.g., `StringRedisTemplate`)
- `@Lazy @Autowired` only for self-injection to work around `@Transactional` proxy issues
- Never use plain `@Autowired` field injection on required dependencies

```java
// GOOD
@RequiredArgsConstructor
public class XxxServiceImpl implements XxxService {
    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;
}

// ALLOWED — optional dependency
@Autowired(required = false)
private StringRedisTemplate stringRedisTemplate;

// ALLOWED — self-injection for @Transactional proxy
@Lazy
@Autowired
private XxxServiceImpl self;
```

### DTO Design

- Use Lombok `@Data` classes, not Java records
- Request DTOs: `@Data` only, with Bean Validation annotations (`@NotBlank`, `@Size`, etc.) and Chinese `message`
- Response DTOs: `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`; use `@SuperBuilder` when the DTO may be subclassed
- DTOs that need Redis/cache serialization must implement `Serializable` with explicit `serialVersionUID`

```java
// Request DTO
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}

// Response DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
}
```

### Entity Design

- All entities extend `BaseEntity` (provides `id`/snowflake, `createTime`, `updateTime`, `isDeleted`)
- Use `@Data` + `@EqualsAndHashCode(callSuper = true)` + `@TableName("table_name")`
- New tables must follow the `id`/`create_time`/`update_time`/`is_deleted` convention (see `BaseEntity`)
- MyBatis-Plus handles camelCase-to-underscore mapping automatically; only use `@TableField` for non-standard column names

### MyBatis-Plus Usage

- Mapper interfaces are empty markers extending `BaseMapper<T>`, annotated with `@Mapper`
- All queries use `LambdaQueryWrapper` / `QueryWrapper` in service code, not custom SQL in mappers
- CRUD services extend `ServiceImpl<Mapper, Entity>` to gain built-in `save`/`getById`/`list`/`update`/`remove`
- Business-only services (no direct entity CRUD) do not extend `ServiceImpl`
- Use `@TableLogic` for logical delete (already in `BaseEntity`), never hard-delete unless explicitly required

### Service Layer

- Every domain service has an interface in `service/` and an implementation in `service/impl/`
- CRUD service interface extends `IService<Entity>`; impl extends `ServiceImpl<Mapper, Entity>` and implements the interface
- Business service interface is a plain Java interface; impl is annotated with `@Service`
- Service methods handle business logic, validation, and entity mapping; controllers should delegate

### Exception Handling

- Domain exceptions use `BusinessException` (extends `RuntimeException`) with `ResultCode` enum
- Business exceptions intentionally return HTTP 200 with error code in JSON body: `Result.error(code, message)`
- Validation errors (`@Valid` failures) return HTTP 400
- Never catch and swallow exceptions; let them propagate to `GlobalExceptionHandler`
- Never expose stack traces or internal details in API responses

### Controller Pattern

- Thin controllers preferred: log → delegate to service → return `Result.success(data)`
- Admin controllers may contain inline CRUD logic for simple admin operations (existing pattern)
- Always use `@Valid @RequestBody` for request parameter validation
- Get current user from `Authentication` parameter: `Long userId = (Long) authentication.getPrincipal()`

## Infrastructure Notes

- Credentials via env vars (`MYSQL_PASSWORD`, `REDIS_PASSWORD`, `API_KEY`, etc.) with dev defaults in `application-dev.yml`.
- AI provider and mock/real mode configured in `application.yml` via `app.ai.provider`, `app.interview.provider`, and `app.ai.mode`/`app.interview.mode`.
- No Docker or docker-compose — services run natively.
- No ESLint/Prettier configured for frontend.
- Development state tracked in `runtime/STATE.md`.
- Development rules and constraints in `runtime/DEVELOPMENT_RULES.txt` — this file must be read before starting any work.
