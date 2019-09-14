package com.github.alex1304.ultimategdbot.core;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandSpec;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="cacheinfo", permLevel=PermissionLevel.BOT_ADMIN)
class CacheInfoCommand {

	private static final String[] STORE_NAMES = { "Channels", "Emojis", "Guilds", "Messages", "Members", "Presences",
			"Roles", "Users", "Voice states" };

	@CommandAction
	public Mono<Void> run(Context ctx) {
		var stateHolder = ctx.getBot().getMainDiscordClient().getServiceMediator().getStateHolder();
		return Mono.zip(
					o -> Arrays.stream(o).map(x -> (Long) x).collect(Collectors.toList()),
					stateHolder.getChannelStore().count(),
					stateHolder.getGuildEmojiStore().count(),
					stateHolder.getGuildStore().count(),
					stateHolder.getMessageStore().count(),
					stateHolder.getMemberStore().count(),
					stateHolder.getPresenceStore().count(),
					stateHolder.getRoleStore().count(),
					stateHolder.getUserStore().count(),
					stateHolder.getVoiceStateStore().count())
				.map(counts -> {
					var sb = new StringBuilder("**__Cache counts:__**\n\n");
					var i = 0;
					for (var count : counts) {
						sb.append("**").append(STORE_NAMES[i]).append("**: ").append(count).append("\n");
						i++;
					}
					sb.append("\n**__Cache implementations:__**\n\n")
						.append("**").append(STORE_NAMES[0]).append("**: `").append(stateHolder.getChannelStore()).append("`\n")
						.append("**").append(STORE_NAMES[1]).append("**: `").append(stateHolder.getGuildEmojiStore()).append("`\n")
						.append("**").append(STORE_NAMES[2]).append("**: `").append(stateHolder.getGuildStore()).append("`\n")
						.append("**").append(STORE_NAMES[3]).append("**: `").append(stateHolder.getMessageStore()).append("`\n")
						.append("**").append(STORE_NAMES[4]).append("**: `").append(stateHolder.getMemberStore()).append("`\n")
						.append("**").append(STORE_NAMES[5]).append("**: `").append(stateHolder.getPresenceStore()).append("`\n")
						.append("**").append(STORE_NAMES[6]).append("**: `").append(stateHolder.getRoleStore()).append("`\n")
						.append("**").append(STORE_NAMES[7]).append("**: `").append(stateHolder.getUserStore()).append("`\n")
						.append("**").append(STORE_NAMES[8]).append("**: `").append(stateHolder.getVoiceStateStore()).append("`\n");
					return sb.toString();
				})
				.flatMap(ctx::reply)
				.then();
	}
}
