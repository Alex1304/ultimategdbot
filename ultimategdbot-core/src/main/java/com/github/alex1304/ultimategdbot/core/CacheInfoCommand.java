package com.github.alex1304.ultimategdbot.core;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;

import reactor.core.publisher.Mono;

@CommandSpec(
		aliases = "cacheinfo",
		shortDescription = "Give statistics on the cache used to store Discord entities.",
		minimumPermissionLevel = PermissionLevel.BOT_ADMIN
)
class CacheInfoCommand {

	private static final String[] STORE_NAMES = { "Channels", "Emojis", "Guilds", "Messages", "Members", "Presences",
			"Roles", "Users", "Voice states" };

	@CommandAction
	@CommandDoc("Check the amount of guilds, roles, messages, etc that the bot is storing. Useful to track down what "
			+ "is consuming the most memory resources during the bot's runtime.")
	public Mono<Void> run(Context ctx) {
		var stateView = ctx.getBot().getGateway().getGatewayResources().getStateView();
		return Mono.zip(
					o -> Arrays.stream(o).map(x -> (Long) x).collect(Collectors.toList()),
					stateView.getChannelStore().count(),
					stateView.getGuildEmojiStore().count(),
					stateView.getGuildStore().count(),
					stateView.getMessageStore().count(),
					stateView.getMemberStore().count(),
					stateView.getPresenceStore().count(),
					stateView.getRoleStore().count(),
					stateView.getUserStore().count(),
					stateView.getVoiceStateStore().count())
				.map(counts -> {
					var sb = new StringBuilder("**__Cache counts:__**\n\n");
					var i = 0;
					for (var count : counts) {
						sb.append("**").append(STORE_NAMES[i]).append("**: ").append(count).append("\n");
						i++;
					}
					return sb.toString();
				})
				.flatMap(ctx::reply)
				.then();
	}
}
