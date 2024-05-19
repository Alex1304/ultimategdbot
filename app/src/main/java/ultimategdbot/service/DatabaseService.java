package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import com.mongodb.reactivestreams.client.MongoClients;
import discord4j.common.jackson.UnknownPropertyHandler;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.mongo.MongoBackend;
import org.immutables.criteria.mongo.MongoSetup;
import org.immutables.criteria.mongo.bson4jackson.BsonModule;
import org.immutables.criteria.mongo.bson4jackson.IdAnnotationModule;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;
import ultimategdbot.config.MongoDBConfig;

@RdiService
public final class DatabaseService {

    private final Backend backend;

    @RdiFactory
    public DatabaseService(ConfigContainer configContainer) {
        final var config = configContainer.get(MongoDBConfig.class);
        final var mapper = new ObjectMapper()
                .registerModule(new BsonModule())
                .registerModule(new Jdk8Module())
                .registerModule(new IdAnnotationModule())
                .addHandler(new UnknownPropertyHandler(true));
        @SuppressWarnings("UnstableApiUsage")
        final var registry = JacksonCodecs.registryFromMapper(mapper);
        final var client = MongoClients.create(config.connectionString());
        final var db = client.getDatabase(config.databaseName()).withCodecRegistry(registry);
        this.backend = new MongoBackend(MongoSetup.of(db));
    }

    public Backend getBackend() {
        return backend;
    }
}
