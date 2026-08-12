# ADR-015: V1 deviations from the original plan

**Status:** Accepted   **Date:** 2026-08-12

## Context
Three items in the V1 build differ from what the approved architecture proposal
described. They are recorded here so future readers do not treat them as drift.

## Decisions

### 1. Core JSON is internal and dependency-free (supersedes "Jayway json-path as a core dependency")
`io.sinaq.api.internal.json` provides a small parser (`Json`), writer (`JsonWriter`,
records supported via reflection) and a JSONPath subset (`JsonPathLite`:
`$.a.b`, `[0]`, `[*]`). Consequence: core keeps ZERO compile-scope dependencies,
which is the property the whole layering rests on. Cost: only a JSONPath subset in
V1; full JSONPath arrives behind the extraction SPI in V2 (ADR-004). Arbitrary POJO
(de)serialization is not supported by the built-in provider — it throws an exception
that names `sinaq-api-jackson`, which is the intended upgrade path.

### 2. `sinaq-api-jdk` promoted into V1 (was: RestAssured-only for V1)
A `java.net.http` engine was added as a first-class module. Reason: it lets the
whole framework be built, tested and shipped without any third-party transport,
and it gives the `HttpEngine` SPI a second implementation — a one-implementation
SPI is an unproven SPI. The starter still ships RestAssured (ADR-013); the JDK
engine is opt-in.

### 3. Integration tests use a JDK `HttpServer`, not WireMock
`MockApiServer` is built on `com.sun.net.httpserver`. Reason: no external
dependency and no download needed, so the suite runs in any environment including
locked-down CI. Cost: no request-matching DSL or stub recording; if stubbing needs
grow, WireMock can be introduced in the integration-tests module only — it never
touches core.

## Consequences
+ Core remains dependency-free through V1
+ HttpEngine SPI validated by two independent implementations
- JSONPath support is a documented subset in V1
