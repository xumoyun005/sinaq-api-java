# Changelog

## 1.2.1 — 2026-08-14

### Fixed
- TestNG / JUnit adapters emit `PASSED` / `FAILED` / `SKIPPED` to match Sinaq Report

## 1.2.0 — 2026-08-13

### Added
- Rich assertion failures: expected vs actual, masked request/response preview, copy-pasteable `curl:`
- IntelliJ live templates (`docs/ide/`)
- RestAssured migration FAQ

### Docs
- Getting started notes richer fail messages

## 1.0.0 — 2026-08-12

First public release (V1–V4 in one version).

### Core
- Fluent HTTP DSL, assertions, retry, polling, soft assertions
- Pluggable engines: JDK `java.net.http`, RestAssured 5, OkHttp 4
- JSONPath (Jayway), JSON Schema, OpenAPI 3 checks
- OAuth2: client credentials, password, refresh token
- YAML config, Jackson POJO serialization
- WireMock stubs, JDBC validation
- Multipart, GraphQL, interceptors, HAR export, contract verify
- Response cache, replay, in-memory message bus, report export
- TestNG listener and JUnit 5 extension

### Modules
Published artifacts use `groupId` `uz.sinaq` (domain `sinaq.uz`), version `1.0.0`.
`examples` and `integration-tests` are not published.
