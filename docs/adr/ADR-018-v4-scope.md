# ADR-018: V4 scope delivered in 0.4.0

**Status:** Accepted   **Date:** 2026-08-12

## Decision

V4 delivers items deferred from V3 and architecture proposal:

- `sinaq-api-wiremock` — WireMock stub wrapper for contract tests
- `sinaq-api-openapi` — lightweight OpenAPI 3 path/status validator
- OAuth2 `password` and `refresh_token` grants
- Core: response cache, replay, distributed runner, messaging SPI, report export, conformance fixtures
- Integration: 500-parallel concurrency gate

## Consequences

+ Contract testing can use WireMock or built-in mock server
+ Cross-language conformance fixture JSON exported from core
- Full OpenAPI request-body schema validation deferred (status/path only in V4)
