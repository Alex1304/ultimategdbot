---
description: Rules for testing code
globs: ["**/src/test/java/**/*.java"]
---

# Testing Rules

## Overview

There is no test suite for Discord commands or Discord4J interaction layers — mocking Discord4J entities is impractical.
Instead, tests focus on the **service layer**, where business logic lives and GD API calls happen.

## Stack

- **JUnit 5** (`org.junit.jupiter:junit-jupiter`) — test framework
- **Reactor Test** (`io.projectreactor:reactor-test`) — `StepVerifier` for asserting `Mono`/`Flux` pipelines
- **Mockito** (`org.mockito:mockito-core` + `org.mockito:mockito-junit-jupiter`) — mocking final classes and services
  that depend on Discord4J entities
- **`GDRouter` test double** — a lambda implementing `GDRouter` that returns hardcoded GD API response strings, injected
  via `GDClient.create().withRouter(router)`

Discord4J entities themselves (e.g. `GatewayDiscordClient`, `InteractionContext`) are never mocked — the Discord
interaction layer is untested. However, services that depend on them (e.g. `EmojiService`) can be mocked with Mockito to
unlock testing of downstream services like `GDLevelService`.

## Setting Up a GDRouter Test Double

`GDRouter` is a single-method interface (`Mono<String> send(GDRequest request)`). Route by URI using a switch
expression:

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

Raw GD response strings for tests are taken from jdash's own test resources at
`<jdash repo root>/client/src/test/resources/`. Each `.txt` file contains the literal server response for a given
operation:

| File                     | Used for                        |
|--------------------------|---------------------------------|
| `searchUsers.txt`        | `gdClient.searchUsers(...)`     |
| `getUserProfile.txt`     | `gdClient.getUserProfile(...)`  |
| `findLevelById.txt`      | `gdClient.findLevelById(...)`   |
| `browseLevels.txt`       | `gdClient.browseLevels(...)`    |
| `downloadLevel.txt`      | `gdClient.downloadLevel(...)`   |
| `getDailyLevelInfo.txt`  | `gdClient.getDailyLevelInfo()`  |
| `getWeeklyDemonInfo.txt` | `gdClient.getWeeklyDemonInfo()` |
| `getSongInfo.txt`        | `gdClient.getSongInfo(...)`     |

Copy the string content directly into test constants. Strip trailing whitespace.

## Instantiating Services Under Test

Services use `@RdiService`/`@RdiFactory` for DI, but in tests construct them directly. Pass `null` for any injected
dependency that the method under test does not use:

```java
// GDUserService.stringToUser() only uses gdClient — null the rest
service = new GDUserService(null, null, null, gdClient, null);
```

Never instantiate services with null for a dependency that the tested method actually calls — this will NPE at runtime.

## Translator

Use `Translator.to(Locale.ENGLISH)` to get an English translator backed by the actual resource bundles (they are on the
test classpath via main resources):

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

## Service Coverage Status

Not all services can be unit-tested without a mocking framework or a live Discord/MongoDB connection. The table below
tracks what is currently covered and what is blocked.

| Service                                                                 | Status  | Reason if blocked                                      |
|-------------------------------------------------------------------------|---------|--------------------------------------------------------|
| `GDUserService.generateAlphanumericToken`                               | Covered | Pure static method                                     |
| `GDUserService.stringToUser`                                            | Covered | GDRouter test double                                   |
| `GDUserService.buildProfile`                                            | Blocked | Uses `GatewayDiscordClient`                            |
| `BlacklistService` (cache read)                                         | Covered | Reflection on private constructor                      |
| `BlacklistService.addToBlacklist` / `removeFromBlacklist`               | Blocked | `BlacklistDao` is `final`, requires MongoDB backend    |
| `GDLevelService.compactEmbed`                                           | Covered | Mockito mock for `EmojiService` + GDRouter test double |
| `GDLevelService.interactiveSearch` / `sendTimelyInfo` / `detailedEmbed` | Blocked | Requires `InteractionContext` (Discord4J)              |
| `EmojiService`                                                          | Blocked | Factory requires `GatewayDiscordClient`                |
| `InteractionLogService`                                                 | Blocked | `InteractionLogDao` requires MongoDB backend           |
| `GDCommandCooldown`                                                     | Blocked | `ConfigContainer` has no test-friendly constructor     |
| `OutputPaginator`                                                       | Blocked | Requires `InteractionContext` (Discord4J)              |
| `DatabaseService`                                                       | Blocked | Requires live MongoDB connection                       |
| `PrivilegeFactory`                                                      | Blocked | Requires `ApplicationInfo` (from Discord)              |
| `ExternalServices`                                                      | Blocked | Requires `GatewayDiscordClient` and `ConfigContainer`  |
| `DefaultTranslator`                                                     | Blocked | Requires `ConfigContainer`; `getLocale()` is trivial   |

### Reflection pattern for services with private constructors

When a service's factory requires an unavailable dependency (e.g. MongoDB-backed DAO) but the logic under test only uses
in-memory state, access the private constructor via reflection:

```java
var constructor = BlacklistService.class.getDeclaredConstructor(BlacklistDao.class, Set.class);
constructor.setAccessible(true);
var service = (BlacklistService) constructor.newInstance(null, new HashSet<>(Set.of(123L)));
```

Pass `null` for constructor parameters the tested method does not call.

## Maven Configuration

Tests live in `app/src/test/java/`. The `app` module has no `module-info` for tests — the main module is `open`, so
JUnit can reflect into it.

To run tests:

```bash
mvn test -pl app
```

Relevant pom.xml entries (already configured):

- Root `pom.xml`: `junit.version`, `plugin.surefire.version` properties; `maven-surefire-plugin`, `junit-jupiter`,
  `reactor-test`, `mockito-core`, `mockito-junit-jupiter` in `dependencyManagement`
- `app/pom.xml`: `junit-jupiter`, `reactor-test`, `mockito-core`, `mockito-junit-jupiter` with `test` scope

### Mockito pattern for services with final-class dependencies

When a service depends on a `final` class (e.g. `EmojiService`) that cannot be instantiated in tests, use
`@ExtendWith(MockitoExtension.class)` and mock it:

```java
@ExtendWith(MockitoExtension.class)
class GDLevelServiceTest {

    @Mock
    private EmojiService emojiService;

    @BeforeEach
    void setUp() {
        when(emojiService.get(anyString())).thenAnswer(inv -> "[" + inv.getArgument(0) + "]");
    }
}
```

Mockito 5.x (the version in use) supports mocking final classes by default — no extra configuration needed.

#### Discord4J internal types

Some Discord4J spec types (e.g. `EmbedCreateSpec.footer()`, `EmbedCreateSpec.author()`) return internal
`ImmutableEmbedCreateFields.*` classes that are inaccessible from the test module. Use `spec.toString()` to assert on
their content rather than calling methods on them directly.
