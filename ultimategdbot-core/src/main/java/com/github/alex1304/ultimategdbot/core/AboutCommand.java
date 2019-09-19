package com.github.alex1304.ultimategdbot.core;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;
import com.github.alex1304.ultimategdbot.api.utils.DiscordFormatter;

import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@CommandSpec(aliases="about")
class AboutCommand {
	
	private final NativePlugin plugin;
	
	public AboutCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@CommandAction
	public Mono<Void> run(Context ctx) {
		return Mono.zip(
				ctx.getBot().getApplicationInfo().zipWhen(ApplicationInfo::getOwner),
				ctx.getBot().getMainDiscordClient().getGuilds().count(),
				ctx.getBot().getMainDiscordClient().getUsers().count(),
				Flux.fromIterable(ctx.getBot().getPlugins())
						.flatMap(p -> p.getGitProperties()
								.map(g -> g.getProperty("git.build.version", "*unknown*"))
								.defaultIfEmpty("*unknown*")
								.map(v -> Tuples.of(p, v)))
						.collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2)))
				.flatMap(TupleUtils.function((appInfoWithOwner, guildCount, userCount, pluginMap) -> {
					var versionInfoBuilder = new StringBuilder("**")
							.append(appInfoWithOwner.getT1().getName())
							.append(" version:** ");
					var nativeGitProps = pluginMap.get(plugin);
					versionInfoBuilder.append(nativeGitProps).append("\n");
					pluginMap.forEach((k, v) -> {
						if (k == this.plugin) return;
						versionInfoBuilder.append("**")
							.append(k.getName())
							.append(" plugin version:** ")
							.append(v)
							.append("\n");
					});
					var vars = new HashMap<String, String>();
					vars.put("bot_name", appInfoWithOwner.getT1().getName());
					vars.put("bot_owner", DiscordFormatter.formatUser(appInfoWithOwner.getT2()));
					vars.put("server_count", "" + guildCount);
					vars.put("user_count", "" + userCount);
					vars.put("version_info", versionInfoBuilder.toString());
					var result = new String[] { plugin.getAboutText() };
					vars.forEach((k, v) -> result[0] = result[0].replaceAll("\\{\\{ *" + k + " *\\}\\}", String.valueOf(v)));
					return ctx.reply(result[0]);
				}))
				.subscribeOn(Schedulers.elastic())
				.then();
	}
}
