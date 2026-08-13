# Migrate from RestAssured to Sinaq

RestAssured has a huge audience — Sinaq is designed so that knowledge **transfers**, not resets.

Goal: your first Sinaq test in **under an hour**, then migrate suite-by-suite.

## 1. Swap the dependency

**Before (RestAssured):**

```xml
<dependency>
  <groupId>io.rest-assured</groupId>
  <artifactId>rest-assured</artifactId>
  <version>5.5.0</version>
  <scope>test</scope>
</dependency>
```

**After (Sinaq starter — includes a RestAssured engine if you want it):**

```xml
<dependency>
  <groupId>uz.sinaq</groupId>
  <artifactId>sinaq-api-starter</artifactId>
  <version>1.2.0</version>
  <scope>test</scope>
</dependency>
```

JUnit 5:

```xml
<dependency>
  <groupId>uz.sinaq</groupId>
  <artifactId>sinaq-api-junit5</artifactId>
  <version>1.2.0</version>
  <scope>test</scope>
</dependency>
```

Optional report:

```xml
<dependency>
  <groupId>uz.sinaq</groupId>
  <artifactId>sinaq-report-java</artifactId>
  <version>1.2.0</version>
  <scope>test</scope>
</dependency>
```

## 2. Replace global config with one client

**RestAssured (static — painful in parallel):**

```java
RestAssured.baseURI = "https://api.example.com";
RestAssured.authentication = oauth2(token);
```

**Sinaq (immutable, share across tests):**

```java
ApiClient api = Sinaq.client()
        .baseUrl("https://api.example.com")
        .engine(new JdkHttpEngine())          // or new RestAssuredEngine()
        .plugin(new JaywayExtractionProvider.Plugin())
        .build();
```

Keep **one** `ApiClient` per suite. Call `api.get(...)` / `api.post(...)` **inside each test**.

### Soft landing: keep RestAssured on the wire

```java
.engine(new RestAssuredEngine())
```

Same Sinaq DSL; transport still RestAssured. Later switch to `JdkHttpEngine` without rewriting tests.

## 3. Side-by-side cheat sheet

| RestAssured | Sinaq |
|-------------|--------|
| `given().when().get("/x")` | `api.get("/x").execute()` |
| `given().when().post("/x")` | `api.post("/x")` … |
| `queryParam("page", 1)` | `.queryParam("page", 1)` |
| `header("X-A", "1")` | `.header("X-A", "1")` |
| `auth().oauth2(token)` / `header("Authorization", "Bearer "+t)` | `.bearer(token)` |
| `body(map)` / `body(pojo)` | `.body(map)` / `.body(pojo)` |
| `then().statusCode(200)` | `.expectStatus(200)` |
| `body("success", equalTo(true))` | `.expect("success", true)` |
| `body("id", notNullValue())` | `.expectNotNull("id")` |
| `body("cards", hasSize(2))` | `.expectArraySize("cards", 2)` or `.expectArraySize("$.cards", 2)` |
| `header("Content-Type", equalTo("…"))` | `.expectHeader("Content-Type", "…")` |
| `extract().path("loanId")` | `.execute().extract("$.loanId")` or `.extract("loanId")` |
| `extract().asString()` | `.execute().body()` / `.text()` |
| `RestAssured.baseURI` | `.baseUrl(...)` on client builder |
| Hamcrest `equalTo` / `hasItem` | Prefer `.expect…` / AssertJ on `extract…` |

Runnable pairs: [`RestAssuredMigrationExample`](../examples/src/test/java/io/sinaq/examples/RestAssuredMigrationExample.java).

## 4. Five patterns you will use every day

### GET + query + status

```java
// RA: given().queryParam("page", 1).when().get("/get").then().statusCode(200);
api.get("/get").queryParam("page", 1).expectStatus(200);
```

### POST + bearer + JSON body + field assert

```java
// RA: given().auth().oauth2(token).body(map).when().post("/post").then().statusCode(200).body(...);
api.post("/post")
        .bearer(token)
        .body(Map.of("amount", 150))
        .expectStatus(200);
```

### Extract a value for the next call

```java
// RA: String id = given()...when().get(...).then().extract().path("id");
String origin = api.get("/get").execute().extract("$.origin");
```

### Header on request

```java
// RA: given().header("X-Request-Id", "abc").when().get("/get")...
api.get("/get").header("X-Request-Id", "abc").expectStatus(200);
```

### List / array size

```java
// RA: then().body("cards", hasSize(2));
api.get("/cards").expectStatus(200).expectArraySize("$.cards", 2);
```

## 5. Migration checklist

1. Add `sinaq-api-starter` (+ `sinaq-api-junit5` if needed).
2. Create shared `ApiClient` (drop `RestAssured.baseURI`).
3. Rewrite **one** smoke test end-to-end.
4. Optionally use `RestAssuredEngine` first.
5. Migrate packages module-by-module (not big-bang).
6. Turn on parallel — Sinaq client is built for it.
7. Optional: `sinaq-report-java` / `@ExtendWith(SinaqReportExtension.class)`.
8. Later: switch engine to `JdkHttpEngine` to drop RestAssured from the classpath.

## 6. What does *not* map 1:1

| RestAssured | In Sinaq |
|-------------|----------|
| Full Hamcrest DSL in `then()` | Built-in `.expect*` + AssertJ/JUnit on extracts |
| `RestAssured.config` / static filters | Client builder + interceptors / plugins |
| Groovy / Spock RA style | Java DSL (primary) |
| Allure RA listener | [Sinaq Report](https://github.com/xumoyun005/sinaq-report) |

You do **not** need a full RestAssured API clone — only the **~80% daily paths** above.

## Migration FAQ

**Q: `RestAssured.baseURI` / static auth?**  
A: Put base URL and defaults on `Sinaq.client()…build()` once. Prefer `.bearer(token)` per request or client defaults — avoid mutable statics for parallel suites.

**Q: Will parallel tests break?**  
A: Share one immutable `ApiClient`; create each request inside the test. Do not mutate RestAssured static config if you still use `RestAssuredEngine`.

**Q: Where did Hamcrest `body("x", hasItem(...))` go?**  
A: Use `.expect` / `.expectArraySize` / extract + AssertJ. Most equality checks are one-liners; complex matchers stay in AssertJ.

**Q: Can I keep RestAssured on the wire?**  
A: Yes — `.engine(new RestAssuredEngine())`, then switch to `JdkHttpEngine` later without rewriting tests.

**Q: IDE snippets?**  
A: Import [docs/ide/Sinaq_API.xml](ide/Sinaq_API.xml) live templates.

## 7. Why switch after you know RA

- No static global config → safer parallel runs  
- Pluggable engines (JDK / OkHttp / RestAssured)  
- First-class events + masking → reporting without leaking secrets  
- Same skills: HTTP, JSONPath, status, auth  
- Richer assertion failures (expected/actual + masked RR + `curl:`)

## See also

- [Getting started](getting-started.md)
- [Publishing / coordinates](publishing.md) (`uz.sinaq`)
- [IDE live templates](ide/README.md)
- Examples module: `mvn -pl examples test`
