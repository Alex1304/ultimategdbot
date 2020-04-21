package com.github.alex1304.ultimategdbot.core;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.Plugin.Builder;
import com.github.alex1304.ultimategdbot.api.PluginBootstrap;
import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.PermissionChecker;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.AnnotatedCommandProvider;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;
import com.github.alex1304.ultimategdbot.core.database.BlacklistedIdDao;
import com.github.alex1304.ultimategdbot.core.database.BotAdminDao;
import com.github.alex1304.ultimategdbot.core.database.CoreConfigDao;
import com.github.alex1304.ultimategdbot.core.database.CoreConfigData;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CorePluginBootstrap implements PluginBootstrap {
	
	private static final String PLUGIN_NAME = "Core";

	@Override
	public Mono<Plugin> setup(Bot bot, PropertyReader pluginProperties) {
		bot.database().configureJdbi(jdbi -> {
			jdbi.getConfig(JdbiImmutables.class).registerImmutable(CoreConfigData.class);
		});
		bot.registerGuildConfigExtension(CoreConfigDao.class);
		var pluginBuilder = Plugin.builder(PLUGIN_NAME)
				.onReady(() -> initBlacklist(bot)
						.and(initPrefixes(bot))
						.and(initMemoryStats()));
		return readAboutText()
						.map(CorePluginBootstrap::initCommandProvider)
						.map(pluginBuilder::setCommandProvider)
						.map(Builder::build);
	}
	
	private static Mono<String> readAboutText() {
		return Mono.fromCallable(() -> String.join("\n", Files.readAllLines(Paths.get(".", "config", "about.txt"))))
				.subscribeOn(Schedulers.boundedElastic());
	}
	
	private static CommandProvider initCommandProvider(String aboutText) {
		// Register commands
		var cmdProvider = new AnnotatedCommandProvider();
		cmdProvider.addAnnotated(new HelpCommand());
		cmdProvider.addAnnotated(new PingCommand());
		cmdProvider.addAnnotated(new SetupCommand());
		cmdProvider.addAnnotated(new LogoutCommand());
		cmdProvider.addAnnotated(new AboutCommand(PLUGIN_NAME, aboutText));
		cmdProvider.addAnnotated(new BotAdminsCommand());
		cmdProvider.addAnnotated(new BlacklistCommand());
		cmdProvider.addAnnotated(new RuntimeCommand());
		// Register permissions
		var permissionChecker = new PermissionChecker();
		permissionChecker.register(PermissionLevel.BOT_OWNER, ctx -> ctx.bot().owner()
				.map(ctx.author()::equals));
		permissionChecker.register(PermissionLevel.BOT_ADMIN, ctx -> ctx.bot().database()
				.withExtension(BotAdminDao.class, dao -> dao.get(ctx.author().getId().asLong()))
				.flatMap(Mono::justOrEmpty)
				.hasElement());
		permissionChecker.register(PermissionLevel.GUILD_OWNER, ctx -> ctx.event().getGuild()
				.map(Guild::getOwnerId)
				.map(ctx.author().getId()::equals));
		permissionChecker.register(PermissionLevel.GUILD_ADMIN, ctx -> ctx.event().getMessage().getChannel()
				.ofType(GuildChannel.class)
				.flatMap(c -> c.getEffectivePermissions(ctx.author().getId())
				.map(ps -> ps.contains(Permission.ADMINISTRATOR))));
		cmdProvider.setPermissionChecker(permissionChecker);
		return cmdProvider;
	}
	
	private static Mono<Void> initBlacklist(Bot bot) {
		return bot.database().withExtension(BlacklistedIdDao.class, BlacklistedIdDao::getAll)
				.flatMapMany(Flux::fromIterable)
				.doOnNext(bot.commandKernel()::blacklist)
				.then();
	}
	
	private static Mono<Void> initPrefixes(Bot bot) {
		return bot.database().withExtension(CoreConfigDao.class, dao -> dao.getAllNonDefaultPrefixes(bot.config().getCommandPrefix()))
				.flatMapMany(Flux::fromIterable)
				.doOnNext(data -> bot.commandKernel().setPrefixForGuild(data.guildId().asLong(), data.prefix().orElseThrow()))
				.then();
	}
	
	private static Mono<Void> initMemoryStats() {
		return Mono.fromRunnable(MemoryStats::start);
	}

	@Override
	public Mono<PropertyReader> initPluginProperties() {
		return Mono.empty();
	}
}
