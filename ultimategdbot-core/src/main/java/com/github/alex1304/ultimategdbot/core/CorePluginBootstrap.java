package com.github.alex1304.ultimategdbot.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.PluginBootstrap;
import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.annotated.AnnotatedCommandProvider;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.util.DatabaseInputFunction;
import com.github.alex1304.ultimategdbot.api.util.DatabaseOutputFunction;
import com.github.alex1304.ultimategdbot.core.database.NativeGuildSettings;

import reactor.core.publisher.Mono;

public class CorePluginBootstrap implements PluginBootstrap {
	
	private static final String PLUGIN_NAME = "Core";

	@Override
	public Mono<Plugin> setup(Bot bot) {
		if (bot.getConfig().isCorePluginDisabled()) {
			return Mono.empty();
		}
		return Mono.fromCallable(() -> String.join("\n", Files.readAllLines(Paths.get(".", "config", "about.txt"))))
				.map(aboutText -> {
					var cmdProvider = new AnnotatedCommandProvider();
					cmdProvider.addAnnotated(new HelpCommand());
					cmdProvider.addAnnotated(new PingCommand());
					cmdProvider.addAnnotated(new SetupCommand());
					cmdProvider.addAnnotated(new SystemCommand());
					cmdProvider.addAnnotated(new AboutCommand(PLUGIN_NAME, aboutText));
					cmdProvider.addAnnotated(new BotAdminsCommand());
					cmdProvider.addAnnotated(new BlacklistCommand());
					cmdProvider.addAnnotated(new CacheInfoCommand());
				})
				.then(Mono.fromRunnable(() -> {
					
					configEntries.put("prefix", new GuildSettingsEntry<>(
							NativeGuildSettings.class,
							NativeGuildSettings::getPrefix,
							NativeGuildSettings::setPrefix,
							(v, guildId) -> DatabaseInputFunction.asIs()
									.withInputCheck(x -> !x.isBlank(), "Cannot be blank")
									.apply(v, guildId)
									.doOnTerminate(() -> bot.getCommandKernel().setPrefixForGuild(guildId, v)),
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
}
