# Sinaq API — Architecture Proposal (Phase 0)

**Status:** APPROVED (2026-08-12). Name changed Xumo → Sinaq after availability/trademark check (R1 resolved).
**Date:** 2026-08-12
**Scope:** Java implementation first (`sinaq-api-java`); Python/.NET follow the same shared specifications.
**Rule:** No implementation code until this document is approved.

---

## 1. Final Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      TEST LAYER (user code)                 │
│        TestNG tests · JUnit 5 tests · plain Java            │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
     ┌──────────▼──────────┐       ┌──────────▼──────────┐
     │   sinaq-api-testng   │       │   sinaq-api-junit5   │
     │ (lifecycle adapter) │       │ (extension adapter) │
     └──────────┬──────────┘       └──────────┬──────────┘
                └──────────────┬──────────────┘
                               │
              ┌────────────────▼────────────────┐
              │          sinaq-api-core          │
              │                                 │
              │  Fluent DSL (ApiClient,         │
              │  RequestSpec, ApiResponse)      │
              │  ───────────────────────────    │
              │  Assertion Engine               │
              │  Extraction (JsonPath)          │
              │  Serialization SPI              │
              │  Auth SPI                       │
              │  Configuration (layered)        │
              │  Context (Test/Request/Exec)    │
              │  Event Model + EventPublisher   │
              │  Masking                        │
              │  Exceptions                     │
              │  ───────────────────────────    │
              │  HTTP ABSTRACTION (SPI):        │
              │  HttpEngine, HttpRequest,       │
              │  HttpResponse, HttpMethod ...   │
              └────────────────┬────────────────┘
                               │ implements HttpEngine
              ┌────────────────▼────────────────┐
              │       sinaq-api-restassured      │
              │  RestAssuredEngine              │
              │  (type translation only)        │
              └────────────────┬────────────────┘
                               │
                        RestAssured 5.x
                               │
                          HTTP / TLS

  Sideways (subscriber, not a dependency of core):
              core events ──▶ EventListener ──▶ Sinaq Report
```

**Dependency direction is one-way, top to bottom.** Core knows only its own SPI interfaces. RestAssured, TestNG, JUnit, and Sinaq Report are all *plugins around* the core, never *inside* it.

---

## 2. Repository Structure

```
sinaq-api-java/
├── pom.xml                      # parent: versions, plugins, enforcer rules
├── sinaq-api-core/               # DSL, models, SPIs — zero test/engine deps
├── sinaq-api-restassured/        # HttpEngine impl over RestAssured
├── sinaq-api-testng/             # TestNG lifecycle adapter
├── sinaq-api-junit5/             # JUnit 5 extension adapter
├── sinaq-api-starter/            # convenience aggregate (core+restassured+testng)
├── examples/                    # runnable examples (not published)
├── integration-tests/           # WireMock-based ITs + concurrency tests (not published)
├── docs/                        # architecture, guides, ADRs (docs/adr/)
└── .github/workflows/ci.yml     # GitHub Actions: mvn clean verify matrix
```

Published artifacts: `core`, `restassured`, `testng`, `junit5`, `starter`. `examples` and `integration-tests` are build-only modules.

---

## 3. Module Dependency Graph

```
sinaq-api-core          → (Jackson databind, json-path, SLF4J-api)   [only these 3rd-party deps]
sinaq-api-restassured   → sinaq-api-core, RestAssured
sinaq-api-testng        → sinaq-api-core, TestNG (provided)
sinaq-api-junit5        → sinaq-api-core, junit-jupiter-api (provided)
sinaq-api-starter       → core + restassured + testng   (documented, nothing hidden)
examples               → starter, junit5
integration-tests      → core + restassured + testng + junit5 + WireMock
```

**Enforced, not just documented:** Maven Enforcer `bannedDependencies` rule — `sinaq-api-core` fails the build if `io.rest-assured:*`, `org.testng:*`, or `org.junit.*:*` appear on its compile classpath. This makes rule §3/§7 of the spec mechanical, not a convention.

Open question for approval: JUnit 5 in the starter or not. Proposal: **starter = core + restassured + testng** (matches Xazna stack); JUnit 5 users add `sinaq-api-junit5` explicitly.

---

## 4. Package Structure (core)

```
io.sinaq.api                  # entry point: Sinaq (factory), ApiClient
io.sinaq.api.client           # client construction, per-client defaults
io.sinaq.api.request          # RequestSpec (builder), immutable ApiRequest
io.sinaq.api.response         # ApiResponse, extraction views
io.sinaq.api.http             # SPI: HttpEngine, HttpRequest, HttpResponse,
                             #      HttpMethod, HttpHeaders, HttpQueryParams,
                             #      HttpPathParams, HttpBody, HttpStatus,
                             #      HttpCookie, HttpTimeout
io.sinaq.api.auth             # AuthProvider SPI + BearerAuth, BasicAuth,
                             #      ApiKeyAuth, CustomAuth
io.sinaq.api.assertion        # assertion engine, matchers, failure formatting
io.sinaq.api.config           # SinaqConfig, layered resolution, env loading
io.sinaq.api.context          # TestContext, RequestContext, ExecutionContext
io.sinaq.api.serialization    # SerializationProvider SPI + Jackson impl
io.sinaq.api.events           # event types, EventPublisher, EventListener
io.sinaq.api.exception        # SinaqException hierarchy
io.sinaq.api.util             # masking, correlation IDs, small helpers
io.sinaq.api.internal.*       # non-API classes; excluded from public API checks
```

Visibility policy: interfaces + final classes in public packages; everything else package-private or under `internal`. `module-info.java` (JPMS) exports only public packages — a second mechanical guard on API surface.

---

## 5. Core Interfaces (signatures, not implementations)

### HTTP SPI

```java
public interface HttpEngine extends AutoCloseable {
    HttpResponse execute(HttpRequest request) throws SinaqEngineException;
    String name();                       // "restassured", later "jdk", "okhttp"
    @Override default void close() {}
}
```

`HttpRequest` / `HttpResponse` are **immutable value objects owned by Sinaq** — engines translate to/from their native types internally and are forbidden from leaking them.

```java
public interface HttpRequest {           // built by core, consumed by engine
    HttpMethod method();
    URI uri();                           // fully resolved: base+path+pathParams+query
    HttpHeaders headers();
    List<HttpCookie> cookies();
    Optional<HttpBody> body();
    HttpTimeout timeout();
    RequestContext context();            // correlationId, requestId, metadata
}

public interface HttpResponse {          // raw transport result, engine → core
    int statusCode();
    Optional<String> statusText();
    HttpHeaders headers();
    List<HttpCookie> cookies();
    byte[] rawBody();
    Duration responseTime();
    Map<String, Object> engineMetadata();
}
```

### Serialization SPI

```java
public interface SerializationProvider {
    String serialize(Object value);
    <T> T deserialize(String content, Class<T> type);
    <T> T deserialize(String content, Type type);
}
```

### Auth SPI

```java
public interface AuthProvider {
    void apply(RequestSpec spec);        // adds headers/params; nothing engine-specific
}
```

### Events SPI

```java
public interface EventListener {
    void onEvent(ReportEvent event);     // exceptions are caught+logged, never break tests
}

public interface EventPublisher {
    void publish(ReportEvent event);
    void register(EventListener listener);
}
```

### Extraction SPI (kept internal-ish in V1, formal SPI in V2)

```java
interface ExtractionProvider {
    <T> T extract(byte[] body, String jsonPath, Class<T> type);
}
```

---

## 6. Public API Design (the DSL)

### Client creation

```java
ApiClient api = Sinaq.client()
    .baseUrl("https://api.example.com")
    .defaultHeader("X-App-Version", "1.0.0")
    .timeout(Duration.ofSeconds(10))
    .engine(new RestAssuredEngine())     // or resolved from starter default
    .build();                            // → immutable, thread-safe
```

### Requests

```java
ApiResponse res = api.post("/users")
    .queryParam("page", 1)
    .pathParam("id", 42)                 // for "/users/{id}" style paths
    .header("X-Trace", traceId)
    .bearer(token)
    .body(user)                          // serialized via SerializationProvider
    .execute();
```

### Assertions — one important design decision

Spec target syntax is `api.get("/users").expectStatus(200)` — assertion directly on the chain. Design:

- `RequestSpec.execute()` → returns `ApiResponse`.
- `ApiResponse` also carries `expect*` methods, each returning `this` (chainable).
- For terseness, `RequestSpec.expectStatus(...)` is sugar: it calls `execute()` internally on first `expect*` and delegates. After that the chain *is* the `ApiResponse`.

```java
api.post("/loan")
   .bearer(token)
   .body(request)
   .expectStatus(200)                    // ← executes here, once
   .expect("success", true)              // JSONPath "$.success"
   .expectNotNull("loanId")
   .expectResponseTimeLessThan(Duration.ofSeconds(2));
```

Semantics: **fail-fast** — first failed assertion throws `SinaqAssertionException` (V2 may add soft-assert mode). Assertion failures never trigger retry.

### Extraction

```java
String id     = res.extract("$.id");
String token  = res.extract("$.accessToken");
User user     = res.as(User.class);
List<String> uuids = res.extractList("$.cards[*].uuid");   // Xazna nested-JSON case
```

Methods on `ApiResponse`: `status()`, `body()`, `rawBody()`, `text()`, `header(name)`, `headers()`, `cookie(name)`, `jsonPath()`, `as(Class)`, `extract(path)`, `responseTime()`, `request()` (immutable reference back), plus the `expect*` family.

---

## 7. HTTP Abstraction Design

- Core builds an immutable `HttpRequest` from `RequestSpec` (URL resolution, path-param substitution, header merging with config defaults, auth application, body serialization) **before** the engine sees anything.
- Engine responsibility is deliberately tiny: transport only. Translate `HttpRequest` → native call → translate native response → `HttpResponse`. No assertions, no serialization, no logging decisions, no config reading.
- `RestAssuredEngine` lives entirely in `sinaq-api-restassured`; it constructs a fresh, non-static RestAssured request per call (never touches `RestAssured` static config — see thread-safety §10 and Risk R3).
- Timeouts: `HttpTimeout(connect, read)` mapped to engine settings; if engine can't honor one, it throws `SinaqConfigurationException` at engine init, not silently ignores.
- V3 readiness: because the SPI is transport-only, `JdkHttpEngine` is a drop-in later with zero core changes.

---

## 8. Event Model

Every event is an immutable record with a shared envelope; the same envelope becomes the **shared report JSON schema** for Python/.NET parity.

```java
ReportEvent {
    EventType type;          // enum below
    Instant   timestamp;
    String    executionId;   // one test run
    String    testId;        // one test method (from adapter, nullable in plain usage)
    String    requestId;     // one HTTP call
    String    correlationId;
    Map<String,Object> payload;   // type-specific, ALREADY MASKED
}

EventType:
TEST_STARTED, REQUEST_CREATED, REQUEST_STARTED, REQUEST_SENT,
RESPONSE_RECEIVED, ASSERTION_STARTED, ASSERTION_PASSED, ASSERTION_FAILED,
TEST_FINISHED, ERROR_OCCURRED
```

Rules:
- Publishing is **synchronous in the calling thread** (deterministic ordering per request; no queue complexity in V1).
- Masking is applied **before** an event object is created — a listener can never observe an unmasked token/PIN/card number.
- Listener exceptions are caught and logged; a broken reporter must never fail a test.
- Payloads carry enough for Sinaq Report V1: method, URL, masked headers, masked body, status, response time, assertion description + expected/actual.

---

## 9. Configuration Model

```
Global Config  (application.properties/yml + system props + env vars)
    ↓ overridden by
Environment Config  (application-int.properties, selected by -Dsinaq.env=int)
    ↓ overridden by
Client Config  (Sinaq.client().baseUrl(...).timeout(...))
    ↓ overridden by
Request Config  (spec.timeout(...), spec.header(...))
```

- Resolution order for external sources: **system property > env var > env-specific file > base file** (documented and unit-tested).
- Result of resolution is one **immutable `SinaqConfig`** snapshot captured at client build time — no live re-reading mid-run (predictable parallel behavior).
- Keys are namespaced: `sinaq.baseUrl`, `sinaq.timeout.connect`, `sinaq.timeout.read`, `sinaq.env`, `sinaq.masking.patterns`, `sinaq.logging.level`, `sinaq.retry.*`.
- Credentials only via env vars / system props / programmatic config — file-based secrets documented as anti-pattern; no defaults containing secrets.
- YAML support: included in V1 **only if** it doesn't force a heavy dependency (SnakeYAML is small — proposal: yes, optional dependency, properties always works).

Retry (V1 minimal): disabled by default; explicit opt-in — `retry(maxAttempts, backoff).on(CONNECT_TIMEOUT, READ_TIMEOUT).onStatus(502, 503)`. Assertion failures are structurally unable to enter the retry path (retry wraps engine execution only, assertions run after).

---

## 10. Thread-Safety Model

| Object | Guarantee |
|---|---|
| `ApiClient`, `SinaqConfig`, `HttpEngine` | Immutable / thread-safe. One instance shared across all parallel tests. |
| `RequestSpec` | Mutable builder, **thread-confined**: create → configure → execute in one thread. Never share, never reuse after `execute()` (documented; `execute()` freezes it into immutable `ApiRequest`). |
| `ApiRequest`, `HttpRequest`, `HttpResponse`, `ApiResponse`, `ReportEvent` | Immutable. |
| Contexts | `ExecutionContext` immutable; `TestContext`/`RequestContext` created per test/request, passed explicitly. Adapters may keep a `ThreadLocal<TestContext>` (the one justified ThreadLocal — TestNG/JUnit callbacks give no other channel), cleared in finally. |
| Static state | None in core. Enforcer + review checklist: no mutable statics, no static RestAssured config. |

Concurrency tests (integration-tests module, WireMock backend): 10 / 100 / 500 parallel requests as CI gates; 1000 as a non-gating stress profile. Assert: no cross-talk between requests (unique marker echo test), no header bleed, correct event `requestId` isolation.

---

## 11. Testing Strategy

| Level | Module | Tool | Gate |
|---|---|---|---|
| Unit | each module | JUnit 5 + AssertJ (test scope only — core's *published* deps stay clean) | Surefire, every phase |
| Integration | integration-tests | WireMock (local mock server; framework never validated against Xazna envs) | Failsafe |
| Contract | integration-tests | Same WireMock suite run against each engine impl (V1: RestAssured only; the suite *is* the engine contract for V3 engines) | Failsafe |
| Concurrency | integration-tests | JUnit 5 parallel + executor harness, 10/100/500 | Failsafe |
| Performance | integration-tests (profile `perf`) | JMH micro-bench: framework overhead vs raw RestAssured call | non-gating, tracked |
| Real API | separate module/profile, later, opt-in | Xazna int env as *validation project*, never in framework CI | manual |

Coverage: JaCoCo, core line coverage gate ≥ 80% (proposal). Tests are independent — no ordering, no shared mutable fixtures. Quality gate per phase = `mvn clean verify` + checklist from spec §35.

Note: using JUnit 5 as the *test-scope* framework inside core's own tests does **not** violate "core must not import JUnit" — that rule applies to core's compile/runtime classpath and public API, which Enforcer checks.

---

## 12. Roadmap

**V1 (0.1.0)** — everything in spec §38: DSL, HTTP abstraction, RestAssured adapter, full request/response model, 4 auth providers, Jackson serialization, assertion engine, JSONPath extraction, layered config + env, timeout, minimal explicit retry, events + masking, TestNG + JUnit 5 adapters, parallel-safe, WireMock ITs, docs.

Phase plan inside V1 (each phase = compile + tests + review + report):
1. Parent POM, enforcer rules, CI skeleton, core exceptions + HTTP SPI types
2. Config + context + masking + events
3. Request model + DSL builder + serialization + auth
4. RestAssuredEngine + first end-to-end GET/POST through WireMock
5. Response model + extraction + assertion engine
6. TestNG + JUnit 5 adapters
7. Retry + timeout hardening + concurrency suite
8. Starter, examples, docs, API review, 0.1.0

**V2** — advanced assertions (arrays, regex, JSON Schema), soft assertions, request templates, environment profiles, OAuth2/JWT + token cache/refresh, polling (`await().until(...)`), test-data utilities, plugin architecture (formal SPI registry), richer report payloads, performance metrics.

**V3** — `JdkHttpEngine` (default), optional OkHttp, RestAssured becomes just one adapter, DB validation, messaging, contract testing, remote/distributed execution, **Java/Python/.NET parity certified by the shared spec test-suite**.

---

## 13. Risks

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R1 | `sinaq` namespace/artifact taken on Maven Central / PyPI / NuGet, or `io.sinaq` domain not owned (Central requires domain proof) | Rename before any publish | Check + register domain **before Phase 1 tags any coordinates publicly**; keep name in one property in parent POM |
| R2 | RestAssured types leak into core over time | Architecture rot | Enforcer bannedDependencies + JPMS exports + review checklist |
| R3 | RestAssured static/global config is not parallel-friendly | Flaky parallel runs | Engine builds per-request `RequestSpecification`, never touches statics; concurrency suite guards it |
| R4 | JSONPath + Jackson as hard core deps conflict with user projects (Gson at Xazna) | Version clashes | Provider SPIs; document BOM; shade nothing; Gson provider possible in V2 |
| R5 | Multi-language parity drift (Python/.NET diverge) | Platform promise breaks | Shared spec = versioned JSON schemas + cross-language conformance fixture suite, written during Java V1, not after |
| R6 | Over-engineering V1 (plugin systems, YAML, retry sophistication) | Never ships | V1 scope frozen to §38; everything else parked in V2 backlog |
| R7 | Report schema instability before Sinaq Report exists | Rework | Mark event schema `0.x`, additive-only changes rule |
| R8 | DSL sugar (`expect*` auto-execute) surprises users (when does the call fire?) | Confusion, double-execution bugs | Single-execution guard in spec object + explicit docs + unit tests |
| R9 | Masking misses a field (PIN, SMS code) | Sensitive data in reports | Deny-by-pattern defaults (Authorization, *token*, *password*, *secret*, pin, cvv, card patterns) + custom patterns + a dedicated masking test class |

---

## 14. ADR List

To be written in `docs/adr/` as short records (skill-standard ADR format); statuses start **Proposed**, flipped to Accepted with your approval of this document:

- **ADR-001** Multi-module Maven layout with hexagonal core (ports & adapters)
- **ADR-002** Own HTTP model + `HttpEngine` SPI; RestAssured as adapter only
- **ADR-003** Java 21 LTS minimum
- **ADR-004** Jackson as default `SerializationProvider`; Jayway json-path for extraction (both behind SPIs)
- **ADR-005** Assertion engine independent of test frameworks; `SinaqAssertionException` + adapter mapping
- **ADR-006** DSL: builder auto-execute on first `expect*`; fail-fast assertions
- **ADR-007** Immutable layered configuration snapshot; precedence order
- **ADR-008** Synchronous in-thread event publishing; mask-before-publish
- **ADR-009** Thread-safety contract (immutable client, thread-confined spec, single justified ThreadLocal in adapters)
- **ADR-010** Retry: transport-only, explicit opt-in, never on assertions
- **ADR-011** WireMock-based validation; real APIs excluded from framework CI
- **ADR-012** SemVer from 0.1.0; additive-only event schema pre-1.0
- **ADR-013** Starter composition (core + restassured + testng) and what it deliberately excludes

---

## Questions needing your decision before Phase 1

1. **Starter contents** — core+restassured+testng as proposed, or include junit5 too?
2. **JSONPath in core** — Jayway json-path as a direct core dependency (simple, V1-pragmatic) vs extraction SPI with the impl in a separate module (purer, more modules). Proposal: direct dep in V1, formal SPI in V2 (ADR-004).
3. **YAML config in V1** — optional SnakeYAML dependency, or properties-only until V2?
4. **Coverage gate** — 80% core acceptable?
5. **Namespace check** — shall I verify `sinaq` availability on Maven Central / PyPI / NuGet now, before Phase 1?

**No implementation begins until you approve this document (or return it with changes).**
