package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.command.annotated.AnnotatedCommand;
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
import reactor.core.publisher.Mono;

/**
 * Provides a set of commands. Each command handler provides their own way to
 * handle errors via a {@link CommandErrorHandler}.
 */
public final class CommandProvider {
	
	private final String name;
	private CommandErrorHandler errorHandler = new CommandErrorHandler();
	private PermissionChecker permissionChecker = new PermissionChecker();
	private final Map<String, Command> commandMap = new HashMap<>();
	private final Map<Class<?>, ParamConverter<?>> paramConverters = initDefaultConverters();
	
	public CommandProvider(String name) {
		this.name = requireNonNull(name);
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
		add(AnnotatedCommand.from(annotated, this));
	}
	
	/**
	 * Provides a command based on a MessageCreateEvent. The event must come with a
	 * message body containing a prefix and the alias of one of the commands
	 * provided by this provider. If it matches with a provided command, arguments
	 * and flags are parsed, and everything is wrapped in an
	 * {@link ExecutableCommand} which is returned. If the event does not match with
	 * any command, an empty {@link Optional} is returned.
	 * 
	 * @param bot        the bot instance
	 * @param prefix     the guild-specific bot prefix
	 * @param flagPrefix the flag prefix
	 * @param locale     the guild-specific locale
	 * @param event      the MessageCreateEvent to process
	 * @param channel    the channel where the event happened
	 * 
	 * @return an ExecutableCommand if the event results in a command to be
	 *         triggered, an empty Optional otherwise.
	 */
	public Mono<ExecutableCommand> provideFromEvent(Bot bot, String prefix, String flagPrefix, Locale locale, MessageCreateEvent event, MessageChannel channel) {
		requireNonNull(bot, "bot cannot be null");
		requireNonNull(prefix, "prefix cannot be null");
		requireNonNull(event, "event cannot be null");
		requireNonNull(channel, "channel cannot be null");
		var botId = bot.gateway().getSelfId().asLong();
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
				.map(cmd -> new ExecutableCommand(cmd, new Context(cmd, event, args, flags, bot, fPrefixUsed, locale, channel), errorHandler));
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
	public PermissionChecker getPermissionChecker() {
		return permissionChecker;
	}

	/**
	 * Sets a custom permission checker. If this method is not called, an empty permission checker will be used.
	 * 
	 * @param permissionChecker the permission checker to set
	 */
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
