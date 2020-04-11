package com.github.alex1304.ultimategdbot.core;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.util.BotUtils;
import com.github.alex1304.ultimategdbot.api.util.SystemUnit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CommandDescriptor(
		aliases = "runtime",
		shortDescription = "Display runtime information on the bot."
)
class RuntimeCommand {

	private static final String[] STORE_NAMES = { "Channels", "Emojis", "Guilds", "Messages", "Members", "Presences",
			"Roles", "Users", "Voice states" };

	@CommandAction
	@CommandDoc("This command allows users to view the resources used by the bot since its startup, "
			+ "such as uptime, RAM usage, Discord events, Discord storage, shard info, etc.")
	public Mono<Void> run(Context ctx) {
		return ctx.channel().typeUntil(
				Mono.zip(objArray -> Flux.fromArray(objArray).cast(EmbedField.class).collectList(),
						uptime(),
						memory(ctx),
						shardInfo(ctx),
						cacheInfo(ctx))
				.flatMap(Function.identity())
				.flatMap(embedFields -> ctx.reply(spec -> spec.setEmbed(embed -> {
					embedFields.forEach(field -> embed.addField(field.title, field.content, false));
					embed.setTimestamp(Instant.now());
				}))))
				.then();
	}

	private Mono<EmbedField> uptime() {
		return Mono.just(new EmbedField("Uptime",
				"The bot has been running for "
						+ BotUtils.formatDuration(Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).withNanos(0))
						+ " without interruption."));
	}
	
	private Mono<EmbedField> memory(Context ctx) {
		return MemoryStats.getStats()
				.map(memStats -> {
					var total = memStats.totalMemory;
					var max = memStats.maxMemory;
					var used = memStats.usedMemory;
					var sb = new StringBuilder();
					sb.append("Maximum system RAM available: ").append(SystemUnit.format(max)).append("\n");
					sb.append("Current JVM size: " + SystemUnit.format(total))
							.append(" (").append(String.format("%.2f", total * 100 / (double) max)).append("%)\n");
					sb.append("Last Garbage Collector run: ")
							.append(memStats.elapsedSinceLastGC()
									.map(t -> BotUtils.formatDuration(t) + " ago")
									.orElse("Never"))
							.append("\n");
					sb.append("Effective RAM usage after last GC run: ").append(SystemUnit.format(used))
							.append(" (").append(String.format("%.2f", used * 100 / (double) max)).append("%)\n");
					return new EmbedField("Memory usage", sb.toString());
				});
	}
	
	private Mono<EmbedField> shardInfo(Context ctx) {
		var shardInfo = ctx.event().getShardInfo();
		return Mono.just(new EmbedField("Gateway sharding info",
				"This chat is served on shard number " + shardInfo.getIndex() + ".\n"
				+ "The bot's gateway connection is currently split over " + shardInfo.getCount() + " shard(s)."));
	}
	
	private Mono<EmbedField> cacheInfo(Context ctx) {
		var stateView = ctx.bot().gateway().getGatewayResources().getStateView();
		return Mono.zip(
				objArray -> Arrays.stream(objArray).map(x -> (Long) x).collect(Collectors.toList()),
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
				var sb = new StringBuilder();
				var i = 0;
				for (var count : counts) {
					sb.append(STORE_NAMES[i]).append(": ").append(count).append("\n");
					i++;
				}
				return sb.toString();
			})
			.map(content -> new EmbedField("Cache usage", content));
	}
	
	private static class EmbedField {
		private final String title;
		private final String content;
		
		private EmbedField(String title, String content) {
			this.title = title;
			this.content = content;
		}
	}
}
