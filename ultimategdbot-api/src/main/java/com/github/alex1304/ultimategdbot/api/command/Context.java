package com.github.alex1304.ultimategdbot.api.command;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

public class Context {
	
	private final Command command;
	private final MessageCreateEvent event;
	private final List<String> args;
	private final Bot bot;
	private final Map<String, Object> variables;
	private final String prefixUsed;
	private final Flags flags;

	public Context(Command command, MessageCreateEvent event, List<String> args, Flags flags, Bot bot, String prefixUsed) {
		this.command = Objects.requireNonNull(command);
		this.event = Objects.requireNonNull(event);
		this.args = Objects.requireNonNull(args);
		this.bot = Objects.requireNonNull(bot);
		this.variables = new ConcurrentHashMap<>();
		this.prefixUsed = Objects.requireNonNull(prefixUsed);
		this.flags = Objects.requireNonNull(flags);
	}
	
	/**
	 * Gets the command that created this context.
	 * 
	 * @return the original command
	 */
	public Command getCommand() {
		return command;
	}

	/**
	 * Gets the message create event associated to this command.
	 *
	 * @return the event
	 */
	public MessageCreateEvent getEvent() {
		return event;
	}

	/**
	 * Gets the arguments of the command.
	 *
	 * @return the args
	 */
	public List<String> getArgs() {
		return args;
	}
	
	public Flags getFlags() {
		return flags;
	}

	/**
	 * Gets the bot instance.
	 * 
	 * @return the bot
	 */
	public Bot getBot() {
		return bot;
	}

	/**
	 * Sends a message in the same channel the command was sent.
	 * 
	 * @param message the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	public Mono<Message> reply(String message) {
		return reply(spec -> spec.setContent(message));
	}
	
	/**
	 * Sends a message in the same channel the command was sent. This method
	 * supports advanced message construction.
	 * 
	 * @param spec the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	public Mono<Message> reply(Consumer<? super MessageCreateSpec> spec) {
		return event.getMessage().getChannel()
				.flatMap(c -> c.createMessage(spec))
				.onErrorResume(ClientException.class, e -> {
					var author = event.getMessage().getAuthor();
					if (e.getStatus().code() != 403 || author.isEmpty()) {
						return Mono.empty();
					}
					return author.get().getPrivateChannel()
							.flatMap(pc -> pc.createMessage("I was unable to send a reply to your command in <#"
									+ event.getMessage().getChannelId().asString()
									+ ">. Make sure that I have permissions to talk and send embeds there.\nError response: `"
									+ e.getErrorResponse() + "`"))
							.onErrorResume(__ -> Mono.empty());
				});
	}

	/**
	 * Gets the prefix used in the command that created this context.
	 * 
	 * @return the prefix used
	 */
	public String getPrefixUsed() {
		return prefixUsed;
	}

	/**
	 * Adds a variable in this context. If a variable of the same name exists, it is
	 * overwritten.
	 * 
	 * @param name the name of the variable
	 * @param val  the value of the variable
	 */
	public void setVar(String name, Object val) {
		variables.put(name, val);
	}

	/**
	 * Adds a variable in this context. If a variable of the same name exists,
	 * nothing happens.
	 * 
	 * @param name the name of the variable
	 * @param val  the value of the variable
	 */
	public void setVarIfNotExists(String name, Object val) {
		if (variables.containsKey(name)) {
			return;
		}
		setVar(name, val);
	}

	/**
	 * Gets the value of a variable.
	 * 
	 * @param name the variable name
	 * @param type the type of the variable
	 * @param      <T> the variable type
	 * @return the value of the variable, or null if not found or exists in the
	 *         wrong type
	 */
	public <T> T getVar(String name, Class<T> type) {
		var val = variables.get(name);
		if (val == null || !type.isInstance(val)) {
			return null;
		}
		return type.cast(val);
	}

	/**
	 * Gets the value of a variable. If not found, the provided default value is
	 * returned instead.
	 * 
	 * @param name       the variable name
	 * @param defaultVal the default value to return if not found
	 * @param      <T> the variable type
	 * @return the value of the variable, or the default value if not found or
	 *         exists in the wrong type
	 */
	@SuppressWarnings("unchecked")
	public <T> T getVarOrDefault(String name, T defaultVal) {
		var val = variables.getOrDefault(name, defaultVal);
		if (!defaultVal.getClass().isInstance(val)) {
			return defaultVal;
		}
		return (T) val;
	}

	@Override
	public String toString() {
		return "Context{"
				+ "command=" + command.getClass().getCanonicalName()
				+ ", event=" + event
				+ ", args=" + args
				+ ", variables=" + variables.keySet()
				+ ", prefixUsed=" + prefixUsed
				+ "}";
	}
}
