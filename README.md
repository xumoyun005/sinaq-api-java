# Sinaq API (Java)

**Sinaq** (Uzbek: *test*) — universal API automation framework for Java.

> **V4 (0.4.0-SNAPSHOT):** WireMock, OpenAPI validation, OAuth password/refresh, messaging, cache, replay, distributed runner. See [docs/v4.md](docs/v4.md).

> **V3 (0.3.0):** OkHttp engine, interceptors, multipart, GraphQL, contract testing, HAR, JDBC validation. See [docs/v3.md](docs/v3.md).

> **V2 (0.2.0):** JSONPath, JSON Schema, soft assertions, polling, templates,
> OAuth2, plugins, metrics. See [docs/v2.md](docs/v2.md).

## Quick start

```java
ApiClient api = Sinaq.client()
        .baseUrl("https://api.example.com")
        .engine(new JdkHttpEngine())
        .plugin(new JaywayExtractionProvider.Plugin())
        .build();

api.post("/loan")
   .bearer(token)
   .body(Map.of("amount", 150))
   .expectStatus(200)
   .expect("success", true)
   .expectNotNull("loanId");
```

## Modules

| Module | Purpose |
|---|---|
| `sinaq-api-core` | DSL, SPIs, config, events — **zero compile deps** |
| `sinaq-api-jdk` | `java.net.http` engine |
| `sinaq-api-restassured` | RestAssured 5 engine |
| `sinaq-api-jackson` | POJO serialization |
| `sinaq-api-jsonpath` | Full Jayway JSONPath (V2) |
| `sinaq-api-jsonschema` | JSON Schema validation (V2) |
| `sinaq-api-oauth` | OAuth2 token provider (V2) |
| `sinaq-api-yaml` | YAML config loader (V2) |
| `sinaq-api-okhttp` | OkHttp transport (V3) |
| `sinaq-api-jdbc` | JDBC DB validation (V3) |
| `sinaq-api-openapi` | OpenAPI 3 validation (V4) |
| `sinaq-api-wiremock` | WireMock stub server (V4) |
| `sinaq-api-testng` / `sinaq-api-junit5` | Test framework adapters |
| `sinaq-api-starter` | V2 aggregate (core+restassured+jackson+jsonpath+jsonschema+testng) |

## Build

Requires **JDK 21+** and Maven 3.9+.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean verify
```
