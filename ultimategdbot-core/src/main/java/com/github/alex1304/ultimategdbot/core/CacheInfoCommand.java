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
		requiredPermissionLevel = PermissionLevel.BOT_ADMIN
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
					sb.append("\n**__Cache implementations:__**\n\n")
						.append("**").append(STORE_NAMES[0]).append("**: `").append(stateView.getChannelStore()).append("`\n")
						.append("**").append(STORE_NAMES[1]).append("**: `").append(stateView.getGuildEmojiStore()).append("`\n")
						.append("**").append(STORE_NAMES[2]).append("**: `").append(stateView.getGuildStore()).append("`\n")
						.append("**").append(STORE_NAMES[3]).append("**: `").append(stateView.getMessageStore()).append("`\n")
						.append("**").append(STORE_NAMES[4]).append("**: `").append(stateView.getMemberStore()).append("`\n")
						.append("**").append(STORE_NAMES[5]).append("**: `").append(stateView.getPresenceStore()).append("`\n")
						.append("**").append(STORE_NAMES[6]).append("**: `").append(stateView.getRoleStore()).append("`\n")
						.append("**").append(STORE_NAMES[7]).append("**: `").append(stateView.getUserStore()).append("`\n")
						.append("**").append(STORE_NAMES[8]).append("**: `").append(stateView.getVoiceStateStore()).append("`\n");
					return sb.toString();
				})
				.flatMap(ctx::reply)
				.then();
	}
}
