package com.github.alex1304.ultimategdbot.core;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.Plugin.Builder;
import com.github.alex1304.ultimategdbot.api.PluginBootstrap;
import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.PermissionChecker;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.AnnotatedCommandProvider;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.util.DatabaseInputFunction;
import com.github.alex1304.ultimategdbot.api.util.DatabaseOutputFunction;
import com.github.alex1304.ultimategdbot.core.database.BlacklistedIds;
import com.github.alex1304.ultimategdbot.core.database.BotAdmins;
import com.github.alex1304.ultimategdbot.core.database.GuildPrefixes;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

public class CorePluginBootstrap implements PluginBootstrap {
	
	private static final String PLUGIN_NAME = "Core";

	@Override
	public Mono<Plugin> setup(Bot bot) {
		var pluginBuilder = Plugin.builder(PLUGIN_NAME)
				.addDatabaseMappingRessources(
						"/BlacklistedIds.hbm.xml",
						"/BotAdmins.hbm.xml",
						"/GuildPrefixes.hbm.xml")
				.addGuildSettingsEntry("prefix", new GuildSettingsEntry<>(
						GuildPrefixes.class,
						GuildPrefixes::getPrefix,
						GuildPrefixes::setPrefix,
						(v, guildId) -> DatabaseInputFunction.asIs()
								.withInputCheck(x -> !x.isBlank(), "Cannot be blank")
								.apply(v, guildId)
								.doOnTerminate(() -> bot.getCommandKernel().setPrefixForGuild(guildId, v)),
						DatabaseOutputFunction.stringValue()))
				.onReady(() -> initBlacklist(bot));
		return bot.getConfig().areCoreCommandsDisabled()
				? Mono.just(pluginBuilder.build())
				: readAboutText()
						.map(CorePluginBootstrap::initCommandProvider)
						.map(pluginBuilder::setCommandProvider)
						.map(Builder::build);
	}
	
	private static Mono<String> readAboutText() {
		return Mono.fromCallable(() -> String.join("\n", Files.readAllLines(Paths.get(".", "config", "about.txt"))));
	}
	
	private static CommandProvider initCommandProvider(String aboutText) {
		// Register commands
		var cmdProvider = new AnnotatedCommandProvider();
		cmdProvider.addAnnotated(new HelpCommand());
		cmdProvider.addAnnotated(new PingCommand());
		cmdProvider.addAnnotated(new SetupCommand());
		cmdProvider.addAnnotated(new SystemCommand());
		cmdProvider.addAnnotated(new AboutCommand(PLUGIN_NAME, aboutText));
		cmdProvider.addAnnotated(new BotAdminsCommand());
		cmdProvider.addAnnotated(new BlacklistCommand());
		cmdProvider.addAnnotated(new CacheInfoCommand());
		// Register permissions
		var permissionChecker = new PermissionChecker();
		permissionChecker.register(PermissionLevel.BOT_OWNER, null, ctx -> ctx.getBot().getOwnerId()
				.map(id -> id.longValue() == ctx.getAuthor().getId().asLong()));
		permissionChecker.register(PermissionLevel.BOT_ADMIN, null, ctx -> ctx.getBot().getDatabase()
				.findByID(BotAdmins.class, ctx.getAuthor().getId().asLong())
				.hasElement());
		permissionChecker.register(PermissionLevel.GUILD_OWNER, null, ctx -> ctx.getEvent().getGuild()
				.map(Guild::getOwnerId)
				.map(ctx.getAuthor().getId()::equals));
		permissionChecker.register(PermissionLevel.GUILD_ADMIN, null, ctx -> ctx.getEvent().getMessage().getChannel()
				.ofType(GuildChannel.class)
				.flatMap(c -> c.getEffectivePermissions(ctx.getAuthor().getId())
				.map(ps -> ps.contains(Permission.ADMINISTRATOR))));
		cmdProvider.setPermissionChecker(permissionChecker);
		return cmdProvider;
	}
	
	private static Mono<Void> initBlacklist(Bot bot) {
		return bot.getDatabase().query(BlacklistedIds.class, "from BlacklistedIds")
				.map(BlacklistedIds::getId)
				.doOnNext(bot.getCommandKernel()::blacklist)
				.then();
	}
}
