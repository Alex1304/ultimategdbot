package com.github.alex1304.ultimategdbot.core;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.Scope;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandPermission;
import com.github.alex1304.ultimategdbot.api.guildconfig.BooleanConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.ConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.ConfigEntryVisitor;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildChannelConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigurator;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildMemberConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildRoleConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.IntegerConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.LongConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.StringConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.ValidationException;
import com.github.alex1304.ultimategdbot.api.util.DiscordFormatter;
import com.github.alex1304.ultimategdbot.api.util.DiscordParser;
import com.github.alex1304.ultimategdbot.api.util.Markdown;
import com.github.alex1304.ultimategdbot.api.util.menu.InteractiveMenu;
import com.github.alex1304.ultimategdbot.api.util.menu.MessageMenuInteraction;
import com.github.alex1304.ultimategdbot.api.util.menu.UnexpectedReplyException;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;

@CommandDescriptor(
	aliases = { "setup", "settings", "configure", "config" },
	shortDescription = "View and edit the bot configuration in this server.",
	scope = Scope.GUILD_ONLY
)
@CommandPermission(level = PermissionLevel.GUILD_ADMIN)
class SetupCommand {

	@CommandAction
	@CommandDoc("Lists all configuration entries available in the bot, listed in alphabetical order and grouped by plugins. "
			+ "Each entry has a unique name with a value associated to it. You can edit an entry using the `set` subcommand.")
	public Mono<Void> run(Context ctx) {
		return ctx.bot().configureGuild(ctx.event().getGuildId().orElseThrow())
				.collectList()
				.flatMap(configurators -> {
					var formattedConfigs = new ArrayList<Mono<String>>();
					var formattedValuePerEntry = new HashMap<ConfigEntry<?>, String>();
					formattedConfigs.add(Mono.just(Markdown.underline(Markdown.bold("Configurable features"))));
					for (var configurator : configurators) {
						var formattedEntries = new ArrayList<Mono<String>>();
						formattedEntries.add(Mono.just(Markdown.underline(configurator.getName())));
						for (var entry : configurator.getConfigEntries()) {
							formattedEntries.add(entry.accept(new DisplayVisitor())
									.defaultIfEmpty("none")
									.doOnNext(displayValue -> formattedValuePerEntry.put(entry, displayValue))
									.map(displayValue -> entry.getDisplayName() + ": " + displayValue)
									.map(Markdown::quote));
						}
						formattedConfigs.add(Flux.concat(formattedEntries)
								.collect(joining("\n")));
					}
					formattedConfigs.add(Mono.just("React with 📝 to edit the configuration for a feature.\n"
							+ "React with 🔄 to reset the configuration for a feature to default values."));
					return Flux.concat(formattedConfigs)
							.collect(joining("\n\n"))
							.map(content -> Tuples.of(configurators, content, formattedValuePerEntry));
				})
				.flatMap(TupleUtils.function((configurators, content, formattedValuePerEntry) -> InteractiveMenu
						.createPaginated(ctx.bot().config().getPaginationControls(), content, 1000)
						.addReactionItem("📝", editInteraction -> {
							editInteraction.closeMenu();
							return handleEditInteraction(ctx, configurators, formattedValuePerEntry, false);
						})
						.addReactionItem("🔄", resetInteraction -> {
							resetInteraction.closeMenu();
							return handleEditInteraction(ctx, configurators, formattedValuePerEntry, true);
						})
						.deleteMenuOnClose(true)
						.open(ctx)));
	}

	private static Mono<Void> handleEditInteraction(Context ctx, List<GuildConfigurator<?>> configurators,
			Map<ConfigEntry<?>, String> formattedValuePerEntry, boolean reset) {
		var sb = new StringBuilder("**Enter the number corresponding to the feature you want to ");
		if (reset) {
			sb.append("reset");
		} else {
			sb.append("setup");
		}
		sb.append(":**\n\n");
		var i = 1;
		for (var configurator : configurators) {
			sb.append(Markdown.code(i + ""))
					.append(": ")
					.append(Markdown.bold(configurator.getName()))
					.append(" - ")
					.append(configurator.getDescription())
					.append('\n');
			i++;
		}
		return InteractiveMenu.createPaginated(ctx.bot().config().getPaginationControls(), sb.toString(), 1000)
				.addMessageItem("", selectInteraction -> {
					int selected;
					try {
						selected = Integer.parseInt(selectInteraction.getArgs().get(0));
					} catch (NumberFormatException e) {
						return Mono.error(new UnexpectedReplyException("Invalid input"));
					}
					if (selected < 1 || selected > configurators.size()) {
						return Mono.error(new UnexpectedReplyException("Feature with number " + selected + " is not listed"));
					}
					selectInteraction.closeMenu();
					var configurator = configurators.get(selected - 1);
					if (reset) {
						return InteractiveMenu.create(Markdown.bold("Are you sure you want to reset all configuration "
										+ "for feature " + configurator.getName() + "?"))
								.addReactionItem("✅", interaction -> {
									return configurator.resetConfig(ctx.bot().database())
											.then(ctx.reply("✅ Configuration has been reset"))
											.then();
								})
								.addReactionItem("🚫", interaction -> Mono.fromRunnable(interaction::closeMenu))
								.open(ctx); 
					}
					return handleSelectedFeatureInteraction(ctx, selectInteraction, configurator, formattedValuePerEntry);
				})
				.deleteMenuOnClose(true)
				.deleteMenuOnTimeout(true)
				.open(ctx);
	}
	
	private static Mono<Void> handleSelectedFeatureInteraction(Context ctx, MessageMenuInteraction selectInteraction,
			GuildConfigurator<?> configurator, Map<ConfigEntry<?>, String> formattedValuePerEntry) {
		var entries = configurator.getConfigEntries().stream()
				.filter(not(ConfigEntry::isReadOnly))
				.collect(toUnmodifiableList());
		if (entries.isEmpty()) {
			return Mono.error(new CommandFailedException("Nothing to configure for this feature"));
		}
		var entryQueue = new ArrayDeque<>(entries);
		var firstEntry = entryQueue.element();
		var valueOfFirstEntry = formattedValuePerEntry.get(firstEntry);
		return firstEntry.accept(new PromptVisitor(configurator, valueOfFirstEntry))
				.<InteractiveMenu>map(InteractiveMenu::create)
				.flatMap(menu -> menu
						.addReactionItem("⏭️", interaction -> goToNextEntry(ctx, entryQueue, formattedValuePerEntry,
								configurator, interaction.getMenuMessage(), interaction::closeMenu))
						.addReactionItem("🚫", __ -> Mono.error(new CommandFailedException("Configuration cancelled")))
						.addMessageItem("", interaction -> {
							var input = interaction.getEvent().getMessage().getContent();
							var currentEntry = entryQueue.element();
							var editEntry = input.equalsIgnoreCase("none")
									? currentEntry.setValue(null)
									: currentEntry.accept(new EditVisitor(ctx.bot(), input))
											.onErrorMap(ValidationException.class,
													e -> new UnexpectedReplyException("The value you provided violates the "
															+ "following constraint: " + e.getMessage()));
							return editEntry.then(goToNextEntry(ctx, entryQueue, formattedValuePerEntry, configurator,
									interaction.getMenuMessage(), interaction::closeMenu));
						})
						.deleteMenuOnClose(true)
						.deleteMenuOnTimeout(true)
						.closeAfterMessage(false)
						.closeAfterReaction(false)
						.open(ctx));
	}

	private static Mono<Void> goToNextEntry(Context ctx, Queue<ConfigEntry<?>> entryQueue,
			Map<ConfigEntry<?>, String> formattedValuePerEntry, GuildConfigurator<?> configurator, Message menuMessage,
			Runnable menuCloser) {
		var goToNextEntry = Mono.fromCallable(entryQueue::element)
				.flatMap(nextEntry -> nextEntry.accept(new PromptVisitor(configurator, formattedValuePerEntry.get(nextEntry)))
						.flatMap(prompt -> menuMessage.edit(spec -> spec.setContent(prompt))))
				.then();
		return Mono.fromRunnable(entryQueue::remove)
				.then(Mono.defer(() -> entryQueue.isEmpty()
						? configurator.saveConfig(ctx.bot().database())
								.then(ctx.reply(":white_check_mark: Configuration done!")
										.and(Mono.fromRunnable(menuCloser)))
						: goToNextEntry));
	}
	
	private static class DisplayVisitor implements ConfigEntryVisitor<String> {

		@Override
		public Mono<String> visit(IntegerConfigEntry entry) {
			return entry.getValue().map(Object::toString);
		}
	
		@Override
		public Mono<String> visit(LongConfigEntry entry) {
			return entry.getValue().map(Object::toString);
		}
	
		@Override
		public Mono<String> visit(BooleanConfigEntry entry) {
			return entry.getValue().map(bool -> bool ? "Yes" : "No");
		}
	
		@Override
		public Mono<String> visit(StringConfigEntry entry) {
			return entry.getValue();
		}
	
		@Override
		public Mono<String> visit(GuildChannelConfigEntry entry) {
			return entry.getValue().map(DiscordFormatter::formatGuildChannel);
		}
	
		@Override
		public Mono<String> visit(GuildRoleConfigEntry entry) {
			return entry.getValue().map(DiscordFormatter::formatRole);
		}
	
		@Override
		public Mono<String> visit(GuildMemberConfigEntry entry) {
			return entry.getValue().map(User::getTag);
		}	
	}
	
	private static class PromptVisitor implements ConfigEntryVisitor<String> {
		
		private final GuildConfigurator<?> configurator;
		private final String currentValue;
		
		private PromptVisitor(GuildConfigurator<?> configurator, String currentValue) {
			this.configurator = configurator;
			this.currentValue = currentValue;
		}

		@Override
		public Mono<String> visit(IntegerConfigEntry entry) {
			return Mono.just(promptSet(entry, "a numeric value"));
		}

		@Override
		public Mono<String> visit(LongConfigEntry entry) {
			return Mono.just(promptSet(entry, "a numeric value"));
		}

		@Override
		public Mono<String> visit(BooleanConfigEntry entry) {
			return Mono.just(promptSet(entry, "Yes or No"));
		}

		@Override
		public Mono<String> visit(StringConfigEntry entry) {
			return Mono.just(promptSet(entry, null));
		}

		@Override
		public Mono<String> visit(GuildChannelConfigEntry entry) {
			return Mono.just(promptSet(entry, "a Discord channel, either by ID, by name or by tag"));
		}

		@Override
		public Mono<String> visit(GuildRoleConfigEntry entry) {
			return Mono.just(promptSet(entry, "a Discord role, either by ID, by name or by tag"));
		}

		@Override
		public Mono<String> visit(GuildMemberConfigEntry entry) {
			return Mono.just(promptSet(entry, "a Discord user present in this server, either by ID, by name or by tag"));
		}
		
		private String promptSet(ConfigEntry<?> entry, String expecting) {
			return Markdown.bold(entry.getDisplayName()) + " (" + configurator.getName() + ")\n"
					+ "Current value: " + currentValue + "\n\n"
					+ Markdown.bold("Enter the new value in the chat to update it"
							+ (expecting == null ? "" : " (expecting " + expecting + ")") + ":") + "\n"
					+ Markdown.italic("React with ⏭️ to skip this configuration entry, or 🚫 to cancel.");
		}
	}
	
	private static class EditVisitor implements ConfigEntryVisitor<Void> {
		
		private final Bot bot;
		private final String input;
		
		private EditVisitor(Bot bot, String input) {
			this.bot = bot;
			this.input = input;
		}

		@Override
		public Mono<Void> visit(IntegerConfigEntry entry) {
			int value;
			try {
				value = Integer.parseInt(input);
			} catch (NumberFormatException e) {
				return Mono.error(new UnexpectedReplyException("Invalid input"));
			}
			return entry.setValue(value);
		}

		@Override
		public Mono<Void> visit(LongConfigEntry entry) {
			long value;
			try {
				value = Long.parseLong(input);
			} catch (NumberFormatException e) {
				return Mono.error(new UnexpectedReplyException("Invalid input"));
			}
			return entry.setValue(value);
		}

		@Override
		public Mono<Void> visit(BooleanConfigEntry entry) {
			if (!input.equalsIgnoreCase("yes") && !input.equalsIgnoreCase("no")) {
				return Mono.error(new UnexpectedReplyException("Expected either Yes or No"));
			}
			return entry.setValue(input.equalsIgnoreCase("yes"));
		}

		@Override
		public Mono<Void> visit(StringConfigEntry entry) {
			return entry.setValue(input);
		}

		@Override
		public Mono<Void> visit(GuildChannelConfigEntry entry) {
			return DiscordParser.parseGuildChannel(bot, entry.getGuildId(), input)
					.flatMap(entry::setValue)
					.onErrorMap(IllegalArgumentException.class, e -> new UnexpectedReplyException(e.getMessage()));
		}

		@Override
		public Mono<Void> visit(GuildRoleConfigEntry entry) {
			return DiscordParser.parseRole(bot, entry.getGuildId(), input)
					.flatMap(entry::setValue)
					.onErrorMap(IllegalArgumentException.class, e -> new UnexpectedReplyException(e.getMessage()));
		}

		@Override
		public Mono<Void> visit(GuildMemberConfigEntry entry) {
			return DiscordParser.parseUser(bot, input)
					.flatMap(user -> user.asMember(entry.getGuildId()))
					.flatMap(entry::setValue)
					.onErrorMap(IllegalArgumentException.class, e -> new UnexpectedReplyException(e.getMessage()));
		}
	}
}
