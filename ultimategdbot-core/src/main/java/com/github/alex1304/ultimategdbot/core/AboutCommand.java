package com.github.alex1304.ultimategdbot.core;

import java.util.HashMap;
import java.util.Objects;

import com.github.alex1304.ultimategdbot.api.command.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.CommandSpec;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.DiscordFormatter;

import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

@CommandSpec(aliases="about")
class AboutCommand {
	
	private final NativePlugin plugin;
	
	public AboutCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@CommandAction
	public Mono<Void> run(Context ctx) {
		return ctx.getBot().getApplicationInfo()
				.zipWhen(ApplicationInfo::getOwner)
				.zipWith(Mono.zip(ctx.getBot().getMainDiscordClient().getGuilds().count(),
						ctx.getBot().getMainDiscordClient().getUsers().count()))
				.flatMap(TupleUtils.function((appInfoWithOwner, guildAndUserCount) -> {
					var versionInfoBuilder = new StringBuilder("**")
							.append(appInfoWithOwner.getT1().getName())
							.append(" version:** ");
					var nativeGitProps = BotUtils.getGitPropertiesForPlugin(plugin);
					versionInfoBuilder.append(nativeGitProps.getProperty("git.build.version", "*unknown*")).append("\n");
					for (var plugin : ctx.getBot().getPlugins()) {
						if (plugin == this.plugin) continue;
						var gitProps = BotUtils.getGitPropertiesForPlugin(plugin);
						versionInfoBuilder.append("**")
								.append(plugin.getName())
								.append(" plugin version:** ")
								.append(gitProps.getProperty("git.build.version", "*unknown*"))
								.append("\n");
						
					}
					var vars = new HashMap<String, String>();
					vars.put("bot_name", appInfoWithOwner.getT1().getName());
					vars.put("bot_owner", DiscordFormatter.formatUser(appInfoWithOwner.getT2()));
					vars.put("server_count", "" + guildAndUserCount.getT1());
					vars.put("user_count", "" + guildAndUserCount.getT2());
					vars.put("version_info", versionInfoBuilder.toString());
					var result = new String[] { plugin.getAboutText() };
					vars.forEach((k, v) -> result[0] = result[0].replaceAll("\\{\\{ *" + k + " *\\}\\}", String.valueOf(v)));
					return ctx.reply(result[0]);
				}))
				.subscribeOn(Schedulers.elastic())
				.then();
	}
}
