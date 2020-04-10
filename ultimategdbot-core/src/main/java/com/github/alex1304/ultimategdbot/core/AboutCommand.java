package com.github.alex1304.ultimategdbot.core;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static reactor.function.TupleUtils.function;

import java.util.HashMap;
import java.util.Properties;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;

import discord4j.common.GitProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@CommandDescriptor(
		aliases = "about",
		shortDescription = "Shows information about the bot itself."
)
class AboutCommand {

	private static final Mono<Properties> D4J_PROPS = Mono.fromCallable(GitProperties::getProperties).cache();

	private final String corePluginName;
	private final String aboutText;
	
	public AboutCommand(String corePluginName, String aboutText) {
		this.corePluginName = requireNonNull(corePluginName);
		this.aboutText = requireNonNull(aboutText);
	}

	@CommandAction
	@CommandDoc("Displays a custom text containing various information about the bot, such as the number "
			+ "of servers the bot is in, the link to add it to your server, the link to the official "
			+ "support server, the version of plugins it uses, and credits to people who contributed to "
			+ "its development.")
	public Mono<Void> run(Context ctx) {
		return Mono.zip(
				D4J_PROPS,
				ctx.bot().owner(),
				ctx.bot().gateway().getSelf(),
				ctx.bot().gateway().getGuilds().count(),
				ctx.bot().gateway().getUsers().count(),
				Flux.fromIterable(ctx.bot().plugins())
						.flatMap(p -> p.getGitProperties()
								.map(g -> g.getProperty("git.build.version", "*unknown*"))
								.defaultIfEmpty("*unknown*")
								.map(v -> Tuples.of(p.getName(), v)))
						.collect(toMap(Tuple2::getT1, Tuple2::getT2)))
				.flatMap(function((d4jProps, botOwner, self, guildCount, userCount, pluginMap) -> {
					var versionInfoBuilder = new StringBuilder("**")
							.append("UltimateGDBot API version:** ");
					var nativeGitProps = pluginMap.get(corePluginName);
					versionInfoBuilder.append(nativeGitProps).append("\n");
					versionInfoBuilder.append("**Discord4J version:** ")
							.append(d4jProps.getProperty(GitProperties.APPLICATION_VERSION))
							.append("\n");
					pluginMap.forEach((k, v) -> {
						if (k.equals(corePluginName)) return;
						versionInfoBuilder.append("**")
							.append(k)
							.append(" plugin version:** ")
							.append(v)
							.append("\n");
					});
					var vars = new HashMap<String, String>();
					vars.put("bot_name", self.getUsername());
					vars.put("bot_owner", botOwner.getTag());
					vars.put("server_count", "" + guildCount);
					vars.put("user_count", "" + userCount);
					vars.put("version_info", versionInfoBuilder.toString());
					var result = new String[] { aboutText };
					vars.forEach((k, v) -> result[0] = result[0].replaceAll("\\{\\{ *" + k + " *\\}\\}", String.valueOf(v)));
					return ctx.reply(result[0]);
				}))
				.subscribeOn(Schedulers.boundedElastic())
				.then();
	}
}
