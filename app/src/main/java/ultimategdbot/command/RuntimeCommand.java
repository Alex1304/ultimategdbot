package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.DurationUtils;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.PrivateCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.sun.management.GarbageCollectionNotificationInfo;
import discord4j.core.spec.EmbedCreateSpec;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import ultimategdbot.Strings;
import ultimategdbot.util.SystemUnit;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

@PrivateCommand
@ChatInputCommand(name = "runtime", description = "Display runtime information on the bot.")
public final class RuntimeCommand implements ChatInputInteractionListener {

    public RuntimeCommand() {
        MemoryStats.start();
    }

    private static Mono<EmbedField> uptime(Translator tr) {
        return Mono.just(new EmbedField(tr.translate(Strings.GENERAL, "uptime"),
                tr.translate(Strings.GENERAL, "uptime_value", DurationUtils.format(
                        Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).withNanos(0)))));
    }

    private static Mono<EmbedField> memory(ChatInputInteractionContext ctx) {
        return MemoryStats.getStats()
                .map(memStats -> {
                    var total = memStats.totalMemory;
                    var max = memStats.maxMemory;
                    var used = memStats.usedMemory;
                    var str = ctx.translate(Strings.GENERAL, "max_ram") + ' ' + SystemUnit.format(max) + "\n" +
                            ctx.translate(Strings.GENERAL, "jvm_size") + ' ' + SystemUnit.format(total) +
                            " (" + String.format("%.2f", total * 100 / (double) max) + "%)\n" +
                            ctx.translate(Strings.GENERAL, "gc_run") + ' ' +
                            memStats.elapsedSinceLastGC()
                                    .map(t -> ctx.translate(Strings.GENERAL, "ago", DurationUtils.format(t)))
                                    .orElse("N/A") +
                            "\n" +
                            ctx.translate(Strings.GENERAL, "ram_after_gc") + ' ' + SystemUnit.format(used) +
                            " (" + String.format("%.2f", used * 100 / (double) max) + "%)\n";
                    return new EmbedField(ctx.translate(Strings.GENERAL, "memory_usage"), str);
                });
    }

    private static Mono<EmbedField> shardInfo(ChatInputInteractionContext ctx) {
        var shardInfo = ctx.event().getShardInfo();
        return Mono.just(new EmbedField(ctx.translate(Strings.GENERAL, "gateway_sharding_info"),
                ctx.translate(Strings.GENERAL, "shard_index", shardInfo.getIndex()) + '\n'
                        + ctx.translate(Strings.GENERAL, "shard_count", shardInfo.getCount())));
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return Mono.zip(objArray -> Flux.fromArray(objArray).cast(EmbedField.class).collectList(),
                        uptime(ctx),
                        memory(ctx),
                        shardInfo(ctx))
                .flatMap(Function.identity())
                .flatMap(embedFields -> {
                    final var embed = EmbedCreateSpec.builder();
                    embedFields.forEach(field -> embed.addField(field.title, field.content, false));
                    embed.timestamp(Instant.now());
                    return ctx.event().createFollowup().withEmbeds(embed.build());
                })
                .then();
    }

    private record EmbedField(String title, String content) {
    }

    private static class MemoryStats {
        private static final Sinks.Many<MemoryStats> STATS_SINK =
                Sinks.many().replay().latestOrDefault(new MemoryStats());
        final long totalMemory;
        final long usedMemory;
        final long maxMemory;
        private final long timestamp;

        private MemoryStats(long timestamp) {
            var total = Runtime.getRuntime().totalMemory();
            var free = Runtime.getRuntime().freeMemory();
            var max = Runtime.getRuntime().maxMemory();
            this.timestamp = timestamp;
            this.totalMemory = total;
            this.usedMemory = total - free;
            this.maxMemory = max;
        }

        private MemoryStats() {
            this.timestamp = -1;
            this.totalMemory = Runtime.getRuntime().totalMemory();
            this.usedMemory = 0;
            this.maxMemory = Runtime.getRuntime().maxMemory();
        }

        static Mono<MemoryStats> getStats() {
            return STATS_SINK.asFlux().next();
        }

        static void start() {
            Flux.<MemoryStats>create(sink -> {
                NotificationListener gcListener = (notif, handback) -> {
                    if (notif.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                        var gcInfo =
                                GarbageCollectionNotificationInfo.from((CompositeData) notif.getUserData()).getGcInfo();
                        sink.next(new MemoryStats(gcInfo.getEndTime()));
                    }
                };
                ManagementFactory.getGarbageCollectorMXBeans()
                        .forEach(bean -> ((NotificationEmitter) bean).addNotificationListener(gcListener, null, null));
            }).subscribe(next -> STATS_SINK.emitNext(next, (signalType, emitResult) -> false));
        }

        Optional<Duration> elapsedSinceLastGC() {
            return Optional.of(timestamp)
                    .filter(t -> t > 0)
                    .map(t -> Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime() - t));
        }
    }
}
