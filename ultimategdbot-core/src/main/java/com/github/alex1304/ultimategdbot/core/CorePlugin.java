package com.github.alex1304.ultimategdbot.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.annotated.AnnotatedCommandProvider;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.DatabaseInputFunction;
import com.github.alex1304.ultimategdbot.api.utils.DatabaseOutputFunction;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

import reactor.core.publisher.Mono;

public class CorePlugin implements Plugin {
	
	private volatile String aboutText;
	private final AnnotatedCommandProvider cmdProvider = new AnnotatedCommandProvider();
	private final Map<String, GuildSettingsEntry<?, ?>> configEntries = new HashMap<String, GuildSettingsEntry<?, ?>>();

	@Override
	public Mono<Void> setup(Bot bot, PropertyParser parser) {
		if (bot.isCorePluginDisabled()) {
			return Mono.empty();
		}
		return Mono.fromCallable(() -> String.join("\n", Files.readAllLines(Paths.get(".", "config", "about.txt"))))
				.doOnNext(aboutText -> this.aboutText = aboutText)
				.and(Mono.fromRunnable(() -> {
					cmdProvider.addAnnotated(new HelpCommand());
					cmdProvider.addAnnotated(new PingCommand());
					cmdProvider.addAnnotated(new SetupCommand());
					cmdProvider.addAnnotated(new SystemCommand());
					cmdProvider.addAnnotated(new AboutCommand(this));
					cmdProvider.addAnnotated(new BotAdminsCommand());
					cmdProvider.addAnnotated(new BlacklistCommand());
					cmdProvider.addAnnotated(new CacheInfoCommand());
					configEntries.put("prefix", new GuildSettingsEntry<>(
							NativeGuildSettings.class,
							NativeGuildSettings::getPrefix,
							NativeGuildSettings::setPrefix,
							(v, guildId) -> DatabaseInputFunction.asIs()
									.withInputCheck(x -> !x.isBlank(), "Cannot be blank")
									.apply(v, guildId)
									.doOnTerminate(() -> bot.getCommandKernel().invalidateCachedPrefixForGuild(guildId)),
							DatabaseOutputFunction.stringValue()
					));
					configEntries.put("server_mod_role", new GuildSettingsEntry<>(
							NativeGuildSettings.class,
							NativeGuildSettings::getServerModRoleId,
							NativeGuildSettings::setServerModRoleId,
							DatabaseInputFunction.toRoleId(bot),
							DatabaseOutputFunction.fromRoleId(bot)
					));
				}));
	}
	
	@Override
	public Mono<Void> onBotReady(Bot bot) {
		return Mono.empty();
	}

	@Override
	public String getName() {
		return "Core";
	}

	@Override
	public Set<String> getDatabaseMappingResources() {
		return Set.of();
	}

	@Override
	public Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries() {
		return configEntries;
	}

	@Override
	public CommandProvider getCommandProvider() {
		return cmdProvider;
	}

	public String getAboutText() {
		return aboutText;
	}
}
