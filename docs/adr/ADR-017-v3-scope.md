# ADR-017: V3 scope delivered in 0.3.0

**Status:** Accepted   **Date:** 2026-08-12

## Decision

V3 ships enterprise automation features as optional modules around the core:

- `sinaq-api-okhttp` — OkHttp 4 transport adapter
- `sinaq-api-jdbc` — JDBC `DbValidator` implementation
- Core: interceptors, multipart, GraphQL DSL, exchange recording, contract verification, HAR export, parallel batch

Integration contract suite extended: GraphQL, multipart, 200-parallel gate, OkHttp engine IT.

## Consequences

+ Three engine options: JDK (default zero-dep), RestAssured, OkHttp
+ Contract/HAR tooling without external report server
- Full OpenAPI/WireMock contract broker deferred to future release
