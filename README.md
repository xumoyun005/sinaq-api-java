# Sinaq API

**Sinaq** (Uzbek: *test*) is a Java framework for HTTP API automation.

Fluent request DSL, pluggable HTTP engines, JSONPath / JSON Schema, OAuth2, WireMock, OpenAPI checks, and TestNG / JUnit 5 adapters. Core has **zero compile-time dependencies**.

**Current version: `1.0.0`** · Maven `groupId`: `uz.sinaq` · Domain: [sinaq.uz](https://sinaq.uz)  
Requires JDK 21+ and Maven 3.9+.

## Install

Maven Central (`groupId` `uz.sinaq`):

```xml
<dependency>
  <groupId>uz.sinaq</groupId>
  <artifactId>sinaq-api-starter</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

`sinaq-api-starter` includes core, RestAssured engine, Jackson, JSONPath, JSON Schema, and TestNG.

Minimal stack (JDK HTTP client, built-in JSON):

```xml
<dependency>
  <groupId>uz.sinaq</groupId>
  <artifactId>sinaq-api-jdk</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

JUnit 5: add `sinaq-api-junit5` as well (starter ships TestNG).

## Coming from RestAssured?

You do not need to relearn API testing. Sinaq maps the everyday RestAssured paths:

| RestAssured | Sinaq |
|-------------|--------|
| `given().when().get("/x")` | `api.get("/x")` |
| `then().statusCode(200)` | `.expectStatus(200)` |
| `body("ok", equalTo(true))` | `.expect("ok", true)` |
| `auth().oauth2(token)` | `.bearer(token)` |
| `extract().path("id")` | `.execute().extract("$.id")` |

**Soft landing:** use `.engine(new RestAssuredEngine())` first — same DSL, RestAssured on the wire — then switch to `JdkHttpEngine` later.

Full side-by-side guide + checklist: **[Migrate from RestAssured](docs/migrate-from-restassured.md)**  
Runnable pairs: `examples/.../RestAssuredMigrationExample.java`

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

Share one `ApiClient` across tests (it is immutable and thread-safe). Create a new request (`api.get(...)`) inside each test.

## Modules

| Module | Purpose |
|--------|---------|
| `sinaq-api-core` | DSL, SPIs, config, events — no compile deps |
| `sinaq-api-jdk` | `java.net.http` engine |
| `sinaq-api-restassured` | RestAssured 5 engine |
| `sinaq-api-okhttp` | OkHttp 4 engine |
| `sinaq-api-jackson` | POJO serialization |
| `sinaq-api-jsonpath` | Jayway JSONPath |
| `sinaq-api-jsonschema` | JSON Schema validation |
| `sinaq-api-oauth` | OAuth2 (client credentials, password, refresh) |
| `sinaq-api-yaml` | YAML config loader |
| `sinaq-api-jdbc` | JDBC assertions |
| `sinaq-api-openapi` | OpenAPI 3 response checks |
| `sinaq-api-wiremock` | WireMock stub server |
| `sinaq-api-testng` | TestNG listener |
| `sinaq-api-junit5` | JUnit 5 extension |
| `sinaq-api-starter` | Common V2 stack (see above) |

## Features

- Fluent HTTP DSL: headers, auth, body, retry, polling, soft assertions
- Engines: JDK, RestAssured, OkHttp
- JSONPath extract, JSON Schema, OpenAPI path/status checks
- OAuth2 token cache; WireMock stubs; JDBC DB checks
- Multipart, GraphQL, interceptors, HAR export, contract verify
- Response cache, replay, in-memory message bus, report export

## Docs

| Guide | Contents |
|-------|----------|
| [Getting started](docs/getting-started.md) | Config, fixtures, retry, adapters |
| [Migrate from RestAssured](docs/migrate-from-restassured.md) | Side-by-side mapping, checklist, soft landing |
| [V2](docs/v2.md) | JSONPath, schema, OAuth, YAML, plugins |
| [V3](docs/v3.md) | OkHttp, interceptors, GraphQL, JDBC, HAR |
| [V4](docs/v4.md) | WireMock, OpenAPI, cache, replay, messaging |

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean verify
```

Runnable examples live in the `examples` module.

## License

Apache License 2.0. See [LICENSE](LICENSE).

Publishing to Maven Central: [docs/publishing.md](docs/publishing.md).
