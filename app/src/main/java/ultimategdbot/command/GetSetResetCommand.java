package ultimategdbot.command;

import botrino.api.util.Markdown;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.grammar.ArgumentMapper;
import botrino.command.grammar.CommandGrammar;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;

import java.util.Set;

abstract class GetSetResetCommand<V> implements Command {

    private final CommandGrammar<Args> grammar = CommandGrammar.builder()
            .nextArgument("value", argumentMapper())
            .build(Args.class);

    abstract ArgumentMapper<V> argumentMapper();

    abstract Mono<String> getFormattedValue(CommandContext ctx);

    abstract Mono<Void> setValue(CommandContext ctx, @Nullable V value);

    abstract String syntax();

    String formatValue(V value) {
        return String.valueOf(value);
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        var docs = documentation(ctx);
        return getFormattedValue(ctx)
                .defaultIfEmpty(Markdown.italic(ctx.translate(Strings.APP, "no_value_assigned")))
                .flatMap(fv -> ctx.channel().createMessage((docs.getDescription() + "\n\n" +
                        ctx.translate(Strings.APP, "current_value_is", fv) + "\n\n" +
                        ctx.translate(Strings.APP, "usage_value_update", ctx.getPrefixUsed() + syntax()) +
                        "\n" +
                        ctx.translate(Strings.APP, "usage_value_reset", ctx.getPrefixUsed() + syntax())).strip()))
                .then();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Command> subcommands() {
        return Set.of(
                Command.builder("set", ctx -> grammar.resolve(ctx)
                        .flatMap(args -> setValue(ctx, (V) args.value)
                                .then(ctx.channel()
                                        .createMessage(ctx.translate(Strings.APP, "value_update_success",
                                                formatValue((V) args.value)))
                                        .then())))
                        .inheritFrom(this)
                        .build(),
                Command.builder("reset", ctx -> setValue(ctx, null)
                        .then(ctx.channel()
                                .createMessage(ctx.translate(Strings.APP, "value_reset_success"))
                                .then()))
                        .inheritFrom(this)
                        .build());
    }

    private final static class Args {
        private Object value;
    }
}
