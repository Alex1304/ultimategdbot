package ultimategdbot.service;

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
import ultimategdbot.database.BlacklistRepository;
import ultimategdbot.database.GuildConfigRepository;

@RdiService
public final class DatabaseService {

    private static final String DATABASE_NAME = "ultimategdbot";

    private final Backend backend;

    @RdiFactory
    public DatabaseService() {
        var mapper = new ObjectMapper()
                .registerModule(new BsonModule())
                .registerModule(new Jdk8Module())
                .registerModule(new IdAnnotationModule())
                .addHandler(new UnknownPropertyHandler(true));
        var registry = JacksonCodecs.registryFromMapper(mapper);
        var client = MongoClients.create();
        var db = client.getDatabase(DATABASE_NAME).withCodecRegistry(registry);
        this.backend = new MongoBackend(MongoSetup.of(db));
    }

    public GuildConfigRepository guildConfigRepository() {
        return new GuildConfigRepository(backend);
    }

    public BlacklistRepository blacklistRepository() {
        return new BlacklistRepository(backend);
    }
}
