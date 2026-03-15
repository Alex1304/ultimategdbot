Create a new MongoDB database entity and its DAO in this project.

Ask the user for:

1. The entity name (PascalCase, e.g. `GdFoo`)
2. The fields: for each field, ask for the name, type, whether it's the `@Criteria.Id` field, and whether it's
   nullable (`Optional<T>`)
3. What DAO query methods are needed (e.g. find by X, get all by Y, save, delete)

Then generate two files:

**1. Entity interface** in `app/src/main/java/ultimategdbot/database/GdFoo.java`:

```java
package ultimategdbot.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.reactor.ReactorReadable;
import org.immutables.criteria.reactor.ReactorWritable;
import org.immutables.value.Value;

@Value.Immutable
@Criteria
@Criteria.Repository(facets = { ReactorReadable.class, ReactorWritable.class })
@JsonSerialize(as = ImmutableGdFoo.class)
@JsonDeserialize(as = ImmutableGdFoo.class)
public interface GdFoo {

    @Criteria.Id
    @JsonProperty("_id")
    long id();  // or whatever the ID field is

    // other fields...
    // nullable fields use Optional<T>
}
```

**2. DAO class** in `app/src/main/java/ultimategdbot/database/GdFooDao.java`:

```java
package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import static ultimategdbot.database.GdFooCriteria.gdFoo;

@RdiService
public final class GdFooDao {

    private final GdFooRepository repository;

    @RdiFactory
    public GdFooDao(DatabaseService db) {
        this.repository = new GdFooRepository(db.getBackend());
    }

    // Generated query methods...
    // Use .oneOrNone() for single results, .fetch() for collections
    // Use .upsert() for saves, repository.delete(criteria) for deletes
    // Partial updates: repository.update(criteria).set(...).execute()
}
```

After generating the files, remind the user that:

1. `ImmutableGdFoo`, `GdFooCriteria`, and `GdFooRepository` are generated automatically by Immutables — do NOT create
   them manually. Run `mvn generate-sources` to regenerate if the interface changes.
2. The DAO is auto-discovered via `@RdiService` and can be injected into any other service or command.
3. The new MongoDB collection name will default to the lowercase class name — verify this matches the intended
   collection name in the database.
