# Testing Rules

## Overview

There is no test suite for Discord commands or Discord4J interaction layers — mocking Discord4J entities is impractical. Instead, tests focus on the **service layer**, where business logic lives and GD API calls happen.

## Stack

- **JUnit 5** (`org.junit.jupiter:junit-jupiter`) — test framework
- **Reactor Test** (`io.projectreactor:reactor-test`) — `StepVerifier` for asserting `Mono`/`Flux` pipelines
- **`GDRouter` test double** — a lambda implementing `GDRouter` that returns hardcoded GD API response strings, injected via `GDClient.create().withRouter(router)`

No mocking framework (e.g. Mockito) is used. Discord4J entities are never tested directly.

## Setting Up a GDRouter Test Double

`GDRouter` is a single-method interface (`Mono<String> send(GDRequest request)`). Route by URI using a switch expression:

```java
GDRouter router = request -> switch (request.getUri()) {
    case GDRequests.GET_GJ_USERS_20 -> Mono.just(searchUsersResponse);
    case GDRequests.GET_GJ_USER_INFO_20 -> Mono.just(getUserProfileResponse);
    default -> Mono.error(new RuntimeException("Unexpected request: " + request.getUri()));
};
GDClient gdClient = GDClient.create().withRouter(router);
```

Use `default -> Mono.error(...)` to fail fast on unexpected requests.

## GD API Response Strings

Raw GD response strings for tests are taken from jdash's own test resources at `<jdash repo root>/client/src/test/resources/`. Each `.txt` file contains the literal server response for a given operation:

| File | Used for |
|---|---|
| `searchUsers.txt` | `gdClient.searchUsers(...)` |
| `getUserProfile.txt` | `gdClient.getUserProfile(...)` |
| `findLevelById.txt` | `gdClient.findLevelById(...)` |
| `browseLevels.txt` | `gdClient.browseLevels(...)` |
| `downloadLevel.txt` | `gdClient.downloadLevel(...)` |
| `getDailyLevelInfo.txt` | `gdClient.getDailyLevelInfo()` |
| `getWeeklyDemonInfo.txt` | `gdClient.getWeeklyDemonInfo()` |
| `getSongInfo.txt` | `gdClient.getSongInfo(...)` |

Copy the string content directly into test constants. Strip trailing whitespace.

## Instantiating Services Under Test

Services use `@RdiService`/`@RdiFactory` for DI, but in tests construct them directly. Pass `null` for any injected dependency that the method under test does not use:

```java
// GDUserService.stringToUser() only uses gdClient — null the rest
service = new GDUserService(null, null, null, gdClient, null);
```

Never instantiate services with null for a dependency that the tested method actually calls — this will NPE at runtime.

## Translator

Use `Translator.to(Locale.ENGLISH)` to get an English translator backed by the actual resource bundles (they are on the test classpath via main resources):

```java
private final Translator translator = Translator.to(Locale.ENGLISH);
```

## Writing Assertions with StepVerifier

```java
// Assert emitted value
StepVerifier.create(service.someMethod(translator, "input"))
        .assertNext(result -> {
            assertEquals("expected", result.field());
        })
        .verifyComplete();

// Assert expected error type
StepVerifier.create(service.someMethod(translator, "bad!input"))
        .expectError(InteractionFailedException.class)
        .verify();

// Assert empty Mono
StepVerifier.create(service.someMethod(translator, "unknown"))
        .verifyComplete();
```

## Test Class Structure

```java
class GDUserServiceTest {

    private static final String SEARCH_USERS_RESPONSE = "...";  // from jdash test resources

    private GDUserService service;
    private final Translator translator = Translator.to(Locale.ENGLISH);

    @BeforeEach
    void setUp() {
        GDRouter router = request -> switch (request.getUri()) {
            case GDRequests.GET_GJ_USERS_20 -> Mono.just(SEARCH_USERS_RESPONSE);
            default -> Mono.error(new RuntimeException("Unexpected: " + request.getUri()));
        };
        service = new GDUserService(null, null, null, GDClient.create().withRouter(router), null);
    }

    @Test
    void methodName_scenario_expectedOutcome() { ... }
}
```

## Maven Configuration

Tests live in `app/src/test/java/`. The `app` module has no `module-info` for tests — the main module is `open`, so JUnit can reflect into it.

To run tests:
```bash
mvn test -pl app
```

Relevant pom.xml entries (already configured):
- Root `pom.xml`: `junit.version`, `plugin.surefire.version` properties; `maven-surefire-plugin`, `junit-jupiter`, `reactor-test` in `dependencyManagement`
- `app/pom.xml`: `junit-jupiter` and `reactor-test` with `test` scope
