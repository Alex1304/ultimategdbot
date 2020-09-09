package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleReader;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.command.annotated.AnnotatedCommand;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.GuildChannelConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.IntConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.LongConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.ParamConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.RoleConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.StringConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.UserConverter;
import com.github.alex1304.ultimategdbot.api.util.MessageUtils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

/**
 * Provides a set of commands. Each command handler provides their own way to
 * handle errors via a {@link CommandErrorHandler}.
 */
public final class CommandProvider {
	
	private static final Logger LOGGER = Loggers.getLogger(CommandProvider.class);
	
	private final String name;
	private CommandErrorHandler errorHandler = new CommandErrorHandler();
	private PermissionChecker permissionChecker = new PermissionChecker();
	private final Map<String, Command> commandMap = new HashMap<>();
	private final Map<Class<?>, ParamConverter<?>> paramConverters = initDefaultConverters();
	
	@Deprecated
	public CommandProvider(String name) {
		this(name, new PermissionChecker());
	}
	
	public CommandProvider(String name, PermissionChecker permissionChecker) {
		this.name = requireNonNull(name);
		this.permissionChecker = permissionChecker;
	}
	
	/**
	 * Gets the name of this command provider.
	 * 
	 * @return the name of this command provider
	 */
	public String getName() {
		return name;
	}

	/**
	 * Adds a command to this provider.
	 * 
	 * @param command the command to add
	 */
	public void add(Command command) {
		requireNonNull(command);
		LOGGER.debug("Added command {} to provider with name \"{}\"", command, name);
		for (var alias : command.getAliases()) {
			commandMap.put(alias, command);
		}
	}
	
	/**
	 * Adds an annotated object as a command into this provider. This is equivalent
	 * to doing:
	 * 
	 * <pre>
	 * provider.add(AnnotatedCommand.from(object, provider));
	 * </pre>
	 * 
	 * @param annotated the annotated object to add
	 */
	public void addAnnotated(Object annotated) {
		add(AnnotatedCommand.from(annotated, this, permissionChecker));
	}
	
	/**
	 * Scans the module path and adds all commands that are found in the given
	 * module. The command's class needs to be annotated with
	 * {@link CommandDescriptor} and have a no-arg constructor.
	 * 
	 * <p>
	 * Since module path scanning requires I/O operations, this method returns a
	 * Flux that subscribes on {@link Schedulers#boundedElastic()}.
	 * 
	 * <p>
	 * This method is equivalent to <code>addAllFromModule(module, null)</code>
	 * 
	 * @param module the Java module where to find the commands
	 * @return a Flux emitting the instances of the annotated command classes,
	 *         created by the module path scanning process. If an error happened
	 *         during the scan, the Flux will error.
	 */
	public Flux<Object> addAllFromModule(Module module) {
		return addAllFromModule(module, null);
	}
	
	/**
	 * Scans the module path and adds all commands that are found in the given
	 * module. The command's class needs to be annotated with
	 * {@link CommandDescriptor} and have a no-arg constructor.
	 * 
	 * <p>
	 * Since module path scanning requires I/O operations, this method returns a
	 * Flux that subscribes on {@link Schedulers#boundedElastic()}.
	 * 
	 * @param module          the Java module where to find the commands
	 * @param classNameFilter a filter to include only certain classes,
	 *                        <code>null</code> to accept all classes
	 * @return a Flux emitting the instances of the annotated command classes,
	 *         created by the module path scanning process. If an error happened
	 *         during the scan, the Flux will error.
	 */
	public Flux<Object> addAllFromModule(Module module, @Nullable Predicate<String> classNameFilter) {
		requireNonNull(module);
		requireNonNull(module.getName(), "must be a named module");
		var classNameFilter0 = requireNonNullElse(classNameFilter, x -> true);
		return Mono.fromCallable(() -> {
					try (ModuleReader moduleReader = module.getLayer().configuration()
							.findModule(module.getName())
							.orElseThrow(() -> new IllegalArgumentException("The given module was "
									+ "not found in the module path"))
							.reference()
							.open()) {
						List<Object> commands = moduleReader.list()
								.filter(resource -> resource.endsWith(".class") && !resource.contains("-"))
								.map(resource -> resource.substring(0, resource.length() - ".class".length())
										.replace(FileSystems.getDefault().getSeparator(), "."))
								.filter(classNameFilter0)
								.map(className -> {
									try {
										return Class.forName(className);
									} catch (ClassNotFoundException e) {
										throw Exceptions.propagate(e);
									}
								})
								.filter(clazz -> clazz.isAnnotationPresent(CommandDescriptor.class))
								.map(clazz -> {
									try { 
										return MethodHandles.publicLookup()
												.findConstructor(clazz, MethodType.methodType(void.class))
												.invoke();
									} catch (Throwable t) {
										throw Exceptions.propagate(t);
									}
								})
								.collect(Collectors.toUnmodifiableList());
						commands.forEach(this::addAnnotated);
						return commands;
					}
				})
				.subscribeOn(Schedulers.boundedElastic())
				.flatMapMany(Flux::fromIterable);
	}
	
	Mono<ExecutableCommand> provideFromEvent(String prefix, String flagPrefix, Locale locale,
			MessageCreateEvent event, MessageChannel channel, PermissionChecker permissionChecker) {
		requireNonNull(prefix, "prefix cannot be null");
		requireNonNull(event, "event cannot be null");
		requireNonNull(channel, "channel cannot be null");
		var botId = event.getClient().getSelfId().asLong();
		var prefixes = Set.of("<@" + botId + ">", "<@!" + botId + ">", prefix);
		var content = event.getMessage().getContent();
		String prefixUsed = null;
		for (var p : prefixes) {
			if (content.toLowerCase().startsWith(p.toLowerCase())) {
				content = content.substring(p.length());
				prefixUsed = p;
				break;
			}
		}
		if (prefixUsed == null) {
			return Mono.empty();
		}
		var parsed = MessageUtils.tokenize(flagPrefix, content);
		var flags = parsed.getT1();
		var args = parsed.getT2();
		if (args.isEmpty()) {
			return Mono.empty();
		}
		final var fPrefixUsed = prefixUsed;
		var command = commandMap.get(args.get(0));
		return Mono.justOrEmpty(command)
				.map(cmd -> new ExecutableCommand(cmd, new Context(cmd, event, args, flags, fPrefixUsed, locale, channel), errorHandler, permissionChecker));
	}
	
	/**
	 * Gets a command instance corresponding to the given alias.
	 *  
	 * @param alias the alias of the command
	 * @return the corresponding command instance, if present
	 */
	public Optional<Command> getCommandByAlias(String alias) {
		requireNonNull(alias);
		return Optional.ofNullable(commandMap.get(alias.toLowerCase()));
	}
	
	/**
	 * Gets the error handler assigned to this provider.
	 * 
	 * @return the error handler
	 */
	public CommandErrorHandler getErrorHandler() {
		return errorHandler;
	}
	
	/**
	 * Sets a custom command handler. If this method is not called, a default handler will be used.
	 * 
	 * @param errorHandler the error handler to set
	 */
	public void setErrorHandler(CommandErrorHandler errorHandler) {
		this.errorHandler = requireNonNull(errorHandler);
	}
	
	/**
	 * Gets the permission checker assigned to this provider.
	 * 
	 * @return the permission checker
	 */
	@Deprecated
	public PermissionChecker getPermissionChecker() {
		return permissionChecker;
	}

	/**
	 * Sets a custom permission checker. If this method is not called, an empty permission checker will be used.
	 * 
	 * @param permissionChecker the permission checker to set
	 */
	@Deprecated
	public void setPermissionChecker(PermissionChecker permissionChecker) {
		this.permissionChecker = requireNonNull(permissionChecker);
	}
	
	/**
	 * Gets all provided commands.
	 * 
	 * @return an unmodifiable Set containing all commands provided by this provider
	 */
	public Set<Command> getProvidedCommands() {
		return unmodifiableSet(new HashSet<>(commandMap.values()));
	}
	
	/**
	 * Adds a new param converter to this annotated command provider.
	 * 
	 * @param converter the converter to add
	 */
	public void addParamConverter(ParamConverter<?> converter) {
		paramConverters.put(converter.type(), converter);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Mono<T> convertParam(Context ctx, String input, Class<T> targetType) {
		var converter = (ParamConverter<T>) paramConverters.get(targetType);
		if (converter == null) {
			return Mono.error(new RuntimeException("No param converter available for the type " + targetType.getName()));
		}
		return converter.convert(ctx, input);
	}
	
	private static Map<Class<?>, ParamConverter<?>> initDefaultConverters() {
		var map = new HashMap<Class<?>, ParamConverter<?>>();
		map.put(String.class, new StringConverter());
		map.put(Integer.class, new IntConverter());
		map.put(int.class, new IntConverter());
		map.put(Long.class, new LongConverter());
		map.put(long.class, new LongConverter());
		map.put(Role.class, new RoleConverter());
		map.put(User.class, new UserConverter());
		map.put(GuildChannel.class, new GuildChannelConverter());
		return map;
	}

	@Override
	public String toString() {
		return "CommandProvider{commandMap=" + commandMap + ", errorHandler=" + errorHandler + "}";
	}
}
