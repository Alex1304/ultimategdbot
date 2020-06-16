package com.github.alex1304.ultimategdbot.api.database;

import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;

import java.sql.Types;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionCallback;
import org.jdbi.v3.core.extension.ExtensionConsumer;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.reactivestreams.Publisher;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Translator;
import com.github.alex1304.ultimategdbot.api.database.guildconfig.GuildConfigDao;
import com.github.alex1304.ultimategdbot.api.database.guildconfig.GuildConfigurator;
import com.github.alex1304.ultimategdbot.api.service.Service;

import discord4j.common.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Database backed by <a href="https://jdbi.org">JDBI</a> with reactive capabilities.
 * 
 */
public final class DatabaseService implements Service {

	private final Bot bot;
	private final Jdbi jdbi;
	private final Set<Class<? extends GuildConfigDao<?>>> guildConfigExtensions = synchronizedSet(new HashSet<>());
	
	private DatabaseService(Bot bot, Jdbi jdbi) {
		this.bot = bot;
		this.jdbi = jdbi;
	}
	
	/**
	 * Creates a new database backed by the given {@link Jdbi} instance.
	 * 
	 * <p>
	 * The given JDBI instance will be enriched as follows:
	 * <ul>
	 * <li>{@link SqlObjectPlugin} will be installed</li>
	 * <li>Column and argument mappers will ba added for the {@link Snowflake} type</li>
	 * <li>{@link SerializableTransactionRunner} will be set as transaction handler</li>
	 * </ul>
	 * 
	 * <p>
	 * It will automatically install the {@link SqlObjectPlugin}, and register
	 * column mappers and arguments for the {@link Snowflake} type. It will also
	 * enable support automatic retrying of failed serializable transactions.
	 * 
	 * @param jdbi the {@link Jdbi} instance backing the database
	 * @return a new {@link DatabaseService}
	 */
	public static DatabaseService create(Bot bot, Jdbi jdbi) {
		requireNonNull(jdbi, "jdbi");
		jdbi.installPlugin(new SqlObjectPlugin());
		jdbi.registerColumnMapper(Snowflake.class, (rs, col, ctx) -> {
			var value = rs.getLong(col);
			if (rs.wasNull()) {
				return null;
			}
			return Snowflake.of(value);
		});
		jdbi.registerArgument(new AbstractArgumentFactory<Snowflake>(Types.BIGINT) {
			@Override
			protected Argument build(Snowflake value, ConfigRegistry config) {
				return (pos, statement, ctx) -> statement.setLong(pos, value.asLong());
			}
		});
		jdbi.setTransactionHandler(new SerializableTransactionRunner());
		return new DatabaseService(bot, jdbi);
	}

	@Override
	public String getName() {
		return "database";
	}
	
	/**
	 * Retrieves all registered configurators for the given guild referenced by its
	 * ID. Empty configuration data will be inserted in database for this specific
	 * guild if it doesn't exist yet. Configurators are emitted in alphabetical case
	 * insensitive order.
	 * 
	 * @param tr      the translator to use to translate strings necessary for the
	 *                configuration
	 * @param guildId the guild ID
	 * @return a Flux emitting all configurators for the guild
	 */
	public Flux<GuildConfigurator<?>> configureGuild(Translator tr, Snowflake guildId) {
		return Flux.defer(() -> {
			if (bot == null) {
				return Mono.error(new IllegalStateException("Service not ready"));
			}
			return Flux.fromIterable(guildConfigExtensions)
					.flatMap(extension -> withExtension(extension, dao -> dao.getOrCreate(guildId.asLong())))
					.<GuildConfigurator<?>>map(data -> data.configurator(tr, bot))
					.sort(Comparator.comparing(GuildConfigurator::getName, String.CASE_INSENSITIVE_ORDER));
		});
	}
	
	/**
	 * Registers a guild configuration extension to this database. This allows to
	 * retrieve all configuration data via the {@link #configureGuild(Snowflake)}
	 * method.
	 * 
	 * @param extension the extension class to register
	 */
	public void registerGuildConfigExtension(Class<? extends GuildConfigDao<?>> extension) {
		guildConfigExtensions.add(extension);
	}

	/**
	 * Allows to access the backing {@link Jdbi} instance to enrich its
	 * configuration, such as registering mappers.
	 * Note that {@link SqlObjectPlugin} is already installed by default.
	 * 
	 * <p>
	 * This method <b>MUST NOT</b> attempt to open any connection to the database.
	 * Database operations should be made using the other methods of this
	 * {@link DatabaseService} class.
	 * 
	 * @param jdbiConsumer the consumer that mutates the backing {@link Jdbi}
	 *                     instance
	 */
	public void configureJdbi(Consumer<Jdbi> jdbiConsumer) {
		requireNonNull(jdbiConsumer, "jdbiConsumer");
		jdbiConsumer.accept(jdbi);
	}
	
	/**
	 * Acquires a {@link Handle} to perform actions on the database. When the
	 * consumer returns, the handle is closed and the returned {@link Mono}
	 * completes. The task is executed on {@link Schedulers#boundedElastic()} as
	 * database interactions are likely to perform blocking I/O operations.
	 * 
	 * <p>
	 * This is suited for when the database operations don't need to return a
	 * result. If you need to return a result, use
	 * {@link #withHandle(HandleCallback)} instead.
	 * 
	 * @param handleConsumer a {@link Consumer} using the handle
	 * @return a Mono that completes when the consumer returns. If an error is
	 *         received, the {@link Mono} will emit {@link DatabaseException} with
	 *         the underlying JDBI exception as the cause
	 */
	public Mono<Void> useHandle(HandleConsumer<?> handleConsumer) {
		requireNonNull(handleConsumer, "handleConsumer");
		return withHandle(handleConsumer.asCallback());
	}

	/**
	 * Acquires a {@link Handle} to perform actions on the database. When the
	 * callback returns, the handle is closed and the returned {@link Mono}
	 * completes. The task is executed on {@link Schedulers#boundedElastic()} as
	 * database interactions are likely to perform blocking I/O operations.
	 * 
	 * <p>
	 * This is suited for when the database operations need to return a result. If
	 * you don't need to return a result, use {@link #useHandle(HandleConsumer)}
	 * instead.
	 * 
	 * @param <T>            the type of the desired return value
	 * @param handleCallback a {@link Consumer} using the handle
	 * @return a Mono that completes when the consumer returns. If an error is
	 *         received, the {@link Mono} will emit {@link DatabaseException} with
	 *         the underlying JDBI exception as the cause
	 */
	public <T> Mono<T> withHandle(HandleCallback<T, ?> handleCallback) {
		requireNonNull(handleCallback, "handleCallback");
		return Mono.fromCallable(() -> jdbi.withHandle(handleCallback))
				.transform(DatabaseService::transform);
	}
	
	/**
	 * Performs a database transaction. When the consumer returns, the trasnaction
	 * is committed and the returned {@link Mono} completes. The task is executed on
	 * {@link Schedulers#boundedElastic()} as database interactions are likely to
	 * perform blocking I/O operations.
	 * 
	 * <p>
	 * This is suited for when the database operations don't need to return a
	 * result. If you need to return a result, use
	 * {@link #withHandle(HandleCallback)} instead.
	 * 
	 * @param txConsumer a {@link Consumer} using the transaction
	 * @return a Mono that completes when the consumer returns. If an error is
	 *         received, the {@link Mono} will emit {@link DatabaseException} with
	 *         the underlying JDBI exception as the cause
	 */
	public Mono<Void> useTransaction(HandleConsumer<?> txConsumer) {
		requireNonNull(txConsumer, "txConsumer");
		return inTransaction(txConsumer.asCallback());
	}

	/**
	 * Performs a database transaction. When the callback returns, the transaction
	 * is committed and the returned {@link Mono} completes. The task is executed on
	 * {@link Schedulers#boundedElastic()} as database interactions are likely to
	 * perform blocking I/O operations.
	 * 
	 * <p>
	 * This is suited for when the database operations need to return a result. If
	 * you don't need to return a result, use {@link #useHandle(HandleConsumer)}
	 * instead.
	 * 
	 * @param <T>        the type of the desired return value
	 * @param txCallback a {@link Consumer} using the transaction
	 * @return a Mono that completes when the consumer returns. If an error is
	 *         received, the {@link Mono} will emit {@link DatabaseException} with
	 *         the underlying JDBI exception as the cause
	 */
	public <T> Mono<T> inTransaction(HandleCallback<T, ?> txCallback) {
		requireNonNull(txCallback, "txCallback");
		return Mono.fromCallable(() -> jdbi.inTransaction(txCallback))
				.transform(DatabaseService::transform);
	}
	
	/**
	 * Uses a JDBI Extension. When the consumer returns, any acquired connections
	 * are closed and the returned {@link Mono} completes. The task is executed on
	 * {@link Schedulers#boundedElastic()} as database interactions are likely to
	 * perform blocking I/O operations.
	 * 
	 * <p>
	 * This is suited for when the use of the extension doesn't need to return a
	 * result. If you need to return a result, use
	 * {@link #withExtension(Class, ExtensionCallback)} instead.
	 * 
	 * @param <E>               the extension type
	 * @param extensionType     the type of extension to use
	 * @param extensionConsumer the consumer that uses the extension
	 * @return a Mono that completes when the consumer returns. If an error is
	 *         received, the {@link Mono} will emit {@link DatabaseException} with
	 *         the underlying JDBI exception as the cause
	 */
	public <E> Mono<Void> useExtension(Class<E> extensionType, ExtensionConsumer<E, ?> extensionConsumer) {
		requireNonNull(extensionType, "extensionType");
		requireNonNull(extensionConsumer, "extensionConsumer");
		return Mono.<Void>fromCallable(() -> {
			jdbi.useExtension(extensionType, extensionConsumer);
			return null;
		}).transform(DatabaseService::transform);
	}

	/**
	 * Uses a JDBI Extension. When the callback returns, any acquired connections
	 * are closed and the returned {@link Mono} completes. The task is executed on
	 * {@link Schedulers#boundedElastic()} as database interactions are likely to
	 * perform blocking I/O operations.
	 * 
	 * <p>
	 * This is suited for when the use of the extension needs to return a result. If
	 * you don't need to return a result, use
	 * {@link #useExtension(Class, ExtensionConsumer)} instead.
	 * 
	 * @param <T>               the return value type
	 * @param <E>               the extension type
	 * @param extensionType     the type of extension to use
	 * @param extensionCallback the callback that uses the extension
	 * @return a Mono that completes when the callback returns. If an error is
	 *         received, the {@link Mono} will emit {@link DatabaseException} with
	 *         the underlying JDBI exception as the cause
	 */
	public <T, E> Mono<T> withExtension(Class<E> extensionType, ExtensionCallback<T, E, ?> extensionCallback) {
		requireNonNull(extensionType, "extensionType");
		requireNonNull(extensionCallback, "extensionCallback");
		return Mono.fromCallable(() -> jdbi.withExtension(extensionType, extensionCallback))
				.transform(DatabaseService::transform);
	}
	
	private static <T> Publisher<T> transform(Publisher<T> publisher) {
		return Flux.from(publisher)
				.subscribeOn(Schedulers.boundedElastic())
				.onErrorMap(DatabaseException::new);
	}
}
