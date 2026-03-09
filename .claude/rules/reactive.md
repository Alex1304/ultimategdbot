---
description: Reactive programming rules that apply across all Java source files
globs: ["**/ultimategdbot/**/*.java"]
---

# Reactive Programming Rules

## Never block

Never call `.block()`, `.blockFirst()`, `.blockLast()`, or any synchronous blocking API inside the `app` module. All I/O is non-blocking.

## Lazy evaluation

Wrap computations that must be deferred (especially those that allocate resources or have side effects) in `Mono.defer()`:

```java
Mono.defer(() -> {
    var result = expensiveComputation();
    return Mono.just(result);
})
```

## Tuple unpacking

When using `Mono.zip()`, unpack tuples with the static import:

```java
import static reactor.function.TupleUtils.function;

Mono.zip(monoA, monoB)
        .map(function((a, b) -> combine(a, b)));
```

## Required values

Use `switchIfEmpty(Mono.error(...))` to fail fast when an expected value is absent:

```java
.switchIfEmpty(Mono.error(new InteractionFailedException(ctx.translate(Strings.GD, "error_key"))))
```

## Error translation

Translate library exceptions into domain exceptions at the service/command boundary:

```java
.onErrorMap(GDClientException.class, e -> new InteractionFailedException(ctx.translate(Strings.GD, "error_gd_unavailable")))
```

Use `.onErrorResume()` only when recovery is genuinely possible; otherwise use `.onErrorMap()`.

## Retry logic

```java
.retryWhen(Retry.indefinitely().filter(RetryableInteractionException.class::isInstance))
// or with backoff for external API calls:
.retryWhen(Retry.backoff(5, Duration.ofSeconds(2)).filter(SomeException.class::isInstance))
```

## Logging in reactive chains

Log inside `.doOnError()` or inside `.onErrorResume()` via `Mono.fromRunnable()` — do not use side-effecting lambdas in `.map()` for logging:

```java
.onErrorResume(e -> Mono.fromRunnable(() -> LOGGER.error("Failed to process", e)))
```
