package com.github.alex1304.ultimategdbot.core;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

class AboutCommand implements Command {
	
	private final NativePlugin plugin;
	
	public AboutCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		return ctx.getBot().getDiscordClients()
				.flatMap(client -> client.getApplicationInfo())
				.next()
				.zipWhen(ApplicationInfo::getOwner)
				.zipWith(Mono.zip(ctx.getBot().getMainDiscordClient().getGuilds().count(),
						ctx.getBot().getMainDiscordClient().getUsers().count()))
				.flatMap(TupleUtils.function((appInfoWithOwner, guildAndUserCount) -> {
					var versionInfoBuilder = new StringBuilder("**UltimateGDBot version:** ");
					var nativeGitProps = BotUtils.getGitPropertiesForPlugin(plugin);
					versionInfoBuilder.append(nativeGitProps.getProperty("git.build.version", "*unknown*")).append("\n");
					for (var plugin : ctx.getBot().getPlugins()) {
						if (plugin == this.plugin) continue;
						var gitProps = BotUtils.getGitPropertiesForPlugin(plugin);
						versionInfoBuilder.append(plugin.getName())
								.append(" plugin version: ")
								.append(gitProps.getProperty("git.build.version", "*unknown*"))
								.append("\n");
						
					}
					var vars = new HashMap<String, String>();
					vars.put("bot_name", appInfoWithOwner.getT1().getName());
					vars.put("bot_owner", BotUtils.formatDiscordUsername(appInfoWithOwner.getT2()));
					vars.put("server_count", "" + guildAndUserCount.getT1());
					vars.put("user_count", "" + guildAndUserCount.getT2());
					vars.put("version_info", versionInfoBuilder.toString());
					var result = new String[] { plugin.getAboutText() };
					vars.forEach((k, v) -> result[0] = result[0].replaceAll("\\{\\{ *" + k + " *\\}\\}", String.valueOf(v)));
					return ctx.reply(result[0]);
				})).then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("about");
	}

	@Override
	public String getDescription() {
		return "Get information about the bot itself.";
	}

	@Override
	public String getLongDescription() {
		return "Displays information such as bot version, support server link, credits, etc.";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
