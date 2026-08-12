# Getting started

## 1. Add the dependency

Common stack (RestAssured + Jackson + TestNG):

```xml
<dependency>
  <groupId>io.sinaq</groupId>
  <artifactId>sinaq-api-starter</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

Zero-dependency stack (JDK transport, built-in serialization):

```xml
<dependency>
  <groupId>io.sinaq</groupId>
  <artifactId>sinaq-api-jdk</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

JUnit 5 users add `sinaq-api-junit5` explicitly (the starter ships TestNG).

## 2. Configure

`src/test/resources/application.properties`:

```properties
sinaq.env=int
sinaq.timeout.connect=5000
sinaq.timeout.read=15000
```

`src/test/resources/application-int.properties`:

```properties
sinaq.baseUrl=https://int-api.example.com
```

Token in CI: `SINAQ_BASEURL`, or pass secrets straight to `.bearer(...)`.

## 3. Build a client once per suite

```java
public class ApiFixture {
    public static final ApiClient API = Sinaq.client()
            .engine(new JdkHttpEngine())
            .defaultHeader("X-App-Version", "1.0.3")
            .build();
}
```

`ApiClient` is immutable and thread-safe — share one instance across parallel tests.
Create a fresh `RequestSpec` (i.e. call `API.get(...)`) inside each test.

## 4. Write tests

```java
@Test
void createLoan() {
    ApiFixture.API.post("/loan")
        .bearer(TokenProvider.get())
        .body(new LoanRequest(150, "UZS"))     // record or Map; POJOs need sinaq-api-jackson
        .expectStatus(200)
        .expect("success", true)
        .expectNotNull("loanId");
}

@Test
void listCards() {
    ApiResponse r = ApiFixture.API.get("/cards").execute();
    List<String> uuids = r.extract("$.cards[*].uuid");
    assertThat(uuids).isNotEmpty();
}
```

## 5. Wire the test framework adapter

TestNG: `@Listeners(SinaqTestNGListener.class)` (or in `testng.xml`).
JUnit 5: `@ExtendWith(SinaqExtension.class)`.

This binds a `TestContext` per thread so every request event carries a `testId`.

## 6. Subscribe to events (optional)

```java
SinaqRuntime.publisher().register(event -> myReporter.write(event));
```

Payloads are already masked. A listener that throws is logged and ignored — a
broken reporter never fails a test.

## 7. Retry (opt-in only)

```java
API.get("/flaky")
   .retry(RetryPolicy.builder().maxAttempts(3).backoff(ofMillis(200)).onStatus(503).build())
   .expectStatus(200);
```

Retry wraps transport execution only. Assertion failures are never retried.

## 8. V2 features (0.2.0)

Add optional modules as needed:

| Module | Use |
|--------|-----|
| `sinaq-api-jsonpath` | Full Jayway JSONPath (`$.cards[*].uuid`) |
| `sinaq-api-jsonschema` | `expectSchema(...)` validation |
| `sinaq-api-oauth` | OAuth2 client-credentials + token cache |
| `sinaq-api-yaml` | `YamlConfigLoader.standardWithYaml()` |

```java
ApiClient api = Sinaq.client()
    .plugin(new JaywayExtractionProvider.Plugin())
    .plugin(new NetworkntSchemaValidator.Plugin())
    .registerTemplate(RequestTemplate.builder("cards", HttpMethod.GET, "/cards").build())
    .build();

api.fromTemplate("cards").execute()
   .soft()
   .expectSchema("{\"type\":\"object\"}")
   .assertAll();

api.get("/status").poll().untilStatus(200).execute();
```

YAML config (`application.yml`) merges with properties — properties win on conflicts.
Use `YamlConfigLoader.standardWithYaml()` instead of `ConfigLoader.standard()`.

See [v2.md](v2.md) for the full V2 guide.

## 9. V3 features (0.3.0)

| Module | Use |
|--------|-----|
| `sinaq-api-okhttp` | `new OkHttpEngine()` transport |
| `sinaq-api-jdbc` | `JdbcDbValidator` + `DbAssertions` |

```java
ExchangeRecorder recorder = new ExchangeRecorder();
SinaqRuntime.publisher().register(recorder);

api.post("/upload").multiPart("name", "demo").expectStatus(200);
api.post("/graphql").graphql("{ users { id } }").expectStatus(200);

ContractVerifier.verify(recorder.exchanges().get(0),
    ContractExpectation.builder().status(200).build());

String har = HarExporter.toHarJson(recorder.exchanges());
```

See [v3.md](v3.md) for the full V3 guide.

## 10. V4 features (0.4.0)

| Module | Use |
|--------|-----|
| `sinaq-api-wiremock` | `SinaqWireMock` stub server |
| `sinaq-api-openapi` | OpenAPI path/status validation |
| OAuth password/refresh | `grantType("password")`, `refreshToken(...)` |

```java
api.get("/health").cacheKey("health-v1").expectStatus(200);
ReplayPlayer.replay(api, recorder.exchanges());

InMemoryMessageBus bus = new InMemoryMessageBus();
bus.awaitMessage("orders", Duration.ofSeconds(2),
    MessageExpectation.builder().bodyContains("\"id\"").build());

new OpenApiValidator(openApiJson).validateResponse("GET", "/health", 200);
```

See [v4.md](v4.md) for the full V4 guide.
