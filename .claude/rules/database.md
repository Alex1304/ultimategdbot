---
description: Rules for database entities and DAOs in the database/ package
globs: ["**/database/**/*.java"]
---

# Database Layer Rules

## Entity interfaces

Every DB entity is an **interface** annotated with all four of the following — never skip any:

```java
@Value.Immutable
@Criteria
@Criteria.Repository(facets = { ReactorReadable.class, ReactorWritable.class })
@JsonSerialize(as = ImmutableFoo.class)
@JsonDeserialize(as = ImmutableFoo.class)
public interface Foo {
```

- The MongoDB ID field must be annotated with both `@Criteria.Id` and `@JsonProperty("_id")`.
- Nullable fields use `Optional<T>` return type — never `@Nullable` on entity methods.
- There are no setters, constructors, or default methods on entity interfaces.

## Immutables-generated code

Never edit `Immutable*`, `*Criteria`, or `*Repository` classes — they are generated from `target/generated-sources/`. To
modify them, change the source interface.

To construct or copy entities, always use the generated builder:

```java
ImmutableFoo.builder().fieldA(x).fieldB(y).build()
ImmutableFoo.copyOf(existing).withFieldA(newValue)
```

## DAO classes

Each entity gets a corresponding `FooDao` class following this exact structure:

```java
@RdiService
public final class FooDao {

    private final FooRepository repository;

    @RdiFactory
    public FooDao(DatabaseService db) {
        this.repository = new FooRepository(db.getBackend());
    }
    // ...
}
```

- Import the generated criteria with a static import: `import static ultimategdbot.database.FooCriteria.foo;`
- Query methods return `Mono<Foo>`, `Flux<Foo>`, or `Mono<WriteResult>` — never block.
- Use `.oneOrNone()` for single-result queries (not `.one()`, which throws on empty).
- Use `.upsert()` for save operations and `.delete(criteria)` for deletes.
- For partial updates, use `repository.update(criteria).set(...).execute()`.
