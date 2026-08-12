# ADR-016: V2 scope delivered in 0.2.0

**Status:** Accepted   **Date:** 2026-08-12

## Decision
V2 features ship as optional modules around the dependency-free core:

- `sinaq-api-jsonpath` — Jayway JSONPath extraction SPI
- `sinaq-api-jsonschema` — NetworkNT schema validation SPI
- `sinaq-api-oauth` — OAuth2 client-credentials + token cache
- `sinaq-api-yaml` — YAML config loader utility

Core adds: soft assertions, polling, templates, profiles, plugins, metrics events,
parallel executor, test-data helpers, advanced assertion methods.

## Consequences
+ Core stays free of Jayway/Jackson/NetworkNT at compile scope
+ Users opt into heavier deps via modules or starter
- Full JSONPath filter tests require jsonpath module on classpath
