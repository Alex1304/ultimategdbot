package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.DurationUtils;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import com.sun.management.GarbageCollectionNotificationInfo;
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

@CommandCategory(CommandCategory.GENERAL)
@Alias("runtime")
@TopLevelCommand
public final class RuntimeCommand implements Command {

    public RuntimeCommand() {
        MemoryStats.start();
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return ctx.channel().typeUntil(
                Mono.zip(objArray -> Flux.fromArray(objArray).cast(EmbedField.class).collectList(),
                        uptime(ctx),
                        memory(ctx),
                        shardInfo(ctx))
                        .flatMap(Function.identity())
                        .flatMap(embedFields -> ctx.channel().createMessage(spec -> spec.setEmbed(embed -> {
                            embedFields.forEach(field -> embed.addField(field.title, field.content, false));
                            embed.setTimestamp(Instant.now());
                        }))))
                .then();
    }

    private static Mono<EmbedField> uptime(Translator tr) {
        return Mono.just(new EmbedField(tr.translate(Strings.APP, "uptime"),
                tr.translate(Strings.APP, "uptime_value", DurationUtils.format(
                        Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).withNanos(0)))));
    }

    private static Mono<EmbedField> memory(CommandContext ctx) {
        return MemoryStats.getStats()
                .map(memStats -> {
                    var total = memStats.totalMemory;
                    var max = memStats.maxMemory;
                    var used = memStats.usedMemory;
                    var str = ctx.translate(Strings.APP, "max_ram") + ' ' + SystemUnit.format(max) + "\n" +
                            ctx.translate(Strings.APP, "jvm_size") + ' ' + SystemUnit.format(total) +
                            " (" + String.format("%.2f", total * 100 / (double) max) + "%)\n" +
                            ctx.translate(Strings.APP, "gc_run") + ' ' +
                            memStats.elapsedSinceLastGC()
                                    .map(t -> ctx.translate(Strings.APP, "ago", DurationUtils.format(t)))
                                    .orElse("N/A") +
                            "\n" +
                            ctx.translate(Strings.APP, "ram_after_gc") + ' ' + SystemUnit.format(used) +
                            " (" + String.format("%.2f", used * 100 / (double) max) + "%)\n";
                    return new EmbedField(ctx.translate(Strings.APP, "memory_usage"), str);
                });
    }

    private static Mono<EmbedField> shardInfo(CommandContext ctx) {
        var shardInfo = ctx.event().getShardInfo();
        return Mono.just(new EmbedField(ctx.translate(Strings.APP, "gateway_sharding_info"),
                ctx.translate(Strings.APP, "shard_index", shardInfo.getIndex()) + '\n'
                        + ctx.translate(Strings.APP, "shard_count", shardInfo.getCount())));
    }

    private static class EmbedField {
        private final String title;
        private final String content;

        private EmbedField(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    private static class MemoryStats {
        private static final Sinks.Many<MemoryStats> STATS_SINK =
                Sinks.many().replay().latestOrDefault(new MemoryStats());

        private final long timestamp;
        final long totalMemory;
        final long usedMemory;
        final long maxMemory;

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

        Optional<Duration> elapsedSinceLastGC() {
            return Optional.of(timestamp)
                    .filter(t -> t > 0)
                    .map(t -> Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime() - t));
        }

        static Mono<MemoryStats> getStats() {
            return STATS_SINK.asFlux().next();
        }

        static void start() {
            Flux.<MemoryStats>create(sink -> {
                NotificationListener gcListener = (notif, handback) -> {
                    if (notif.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                        var gcInfo = GarbageCollectionNotificationInfo.from((CompositeData) notif.getUserData()).getGcInfo();
                        sink.next(new MemoryStats(gcInfo.getEndTime()));
                    }
                };
                ManagementFactory.getGarbageCollectorMXBeans()
                        .forEach(bean -> ((NotificationEmitter) bean).addNotificationListener(gcListener, null, null));
            }).subscribe(next -> STATS_SINK.emitNext(next, (signalType, emitResult) -> false));
        }
    }
}
