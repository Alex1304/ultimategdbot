package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.DurationUtils;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;

import java.time.Duration;

import static reactor.function.TupleUtils.function;

@CommandCategory(CommandCategory.GENERAL)
@TopLevelCommand
@Alias("ping")
public final class PingCommand implements Command {

    private static String computeLatency(Translator tr, MessageCreateEvent event, long apiLatency) {
        return tr.translate(Strings.GENERAL, "pong") + '\n'
                + tr.translate(Strings.GENERAL, "api_latency") + ' ' + DurationUtils.format(Duration.ofMillis(apiLatency)) + "\n"
                + tr.translate(Strings.GENERAL, "gateway_latency") + ' ' + event.getClient()
                .getGatewayClient(event.getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime)
                .map(DurationUtils::format)
                .orElse(tr.translate(Strings.GENERAL, "unknown"));
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return ctx.channel().createMessage(ctx.translate(Strings.GENERAL, "pong"))
                .elapsed()
                .flatMap(function((apiLatency, message) -> message.edit()
                        .withContentOrNull(computeLatency(ctx, ctx.event(), apiLatency))))
                .then();
    }
}
