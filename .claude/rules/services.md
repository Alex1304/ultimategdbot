---
description: Rules for the service layer in the service/ package
globs: ["**/service/**/*.java"]
---

# Service Layer Rules

## Class structure

```java
@RdiService
public final class FooService {

    private static final Logger LOGGER = Loggers.getLogger(FooService.class);

    private final BarService bar;

    @RdiFactory
    public FooService(BarService bar) {
        this.bar = bar;
    }
```

- `@RdiService` on the class, `@RdiFactory` on the constructor — no exceptions.
- The class must be `final` and all fields `final`.
- Logger via `Loggers.getLogger(ClassName.class)` (from `reactor.util.Loggers`) — not `LoggerFactory`.

## Return types

- Async operations return `Mono<T>` or `Flux<T>` — never a plain `T` that requires blocking.
- Synchronous helpers (pure computation, no I/O) may return plain types.
- Use `@Nullable` (from `org.jspecify.annotations.Nullable`) for parameters that may be null; do not use `Optional` as a parameter type. Note: `reactor.util.annotation.Nullable` is deprecated.

## Registration

Services annotated with `@RdiService` are auto-discovered. Services that cannot use `@RdiService` (e.g., external library types like `GDClient`, `ApplicationInfo`) must be manually registered as `ServiceDescriptor` entries in `UltimateGDBotExtension.provideExtraServices()` using `externalStaticFactory`.

## Background processing

If a service needs to process events asynchronously in a background loop, use `Sinks.Many<T>`:

```java
private final Sinks.Many<Event> sink = Sinks.many().unicast().onBackpressureBuffer();

public FooService(...) {
    // deps...
    run(); // start the loop in the constructor
}

private void run() {
    sink.asFlux()
            .flatMap(event -> process(event)
                    .onErrorResume(e -> Mono.fromRunnable(() -> LOGGER.error("...", e))))
            .subscribe();
}
```

Never use `Thread`, `ExecutorService`, or `CompletableFuture` — stay in Reactor.
