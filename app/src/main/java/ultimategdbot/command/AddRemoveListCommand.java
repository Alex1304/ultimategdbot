package ultimategdbot.command;

import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.grammar.ArgumentMapper;
import botrino.command.grammar.CommandGrammar;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.OutputPaginator;

import java.util.Set;

abstract class AddRemoveListCommand<E> implements Command {

    final OutputPaginator outputPaginator;

    private final CommandGrammar<Args> grammar = CommandGrammar.builder()
            .nextArgument("item", argumentMapper())
            .build(Args.class);

    AddRemoveListCommand(OutputPaginator outputPaginator) {
        this.outputPaginator = outputPaginator;
    }

    abstract ArgumentMapper<E> argumentMapper();

    abstract Mono<Void> add(CommandContext ctx, E item);

    abstract Mono<Void> remove(CommandContext ctx, E item);

    abstract Flux<String> listFormattedItems(CommandContext ctx);

    String formatItem(E item) {
        return String.valueOf(item);
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        var docs = documentation(ctx);
        return listFormattedItems(ctx).collectList().flatMap(list -> outputPaginator.paginate(ctx, list,
                content -> docs.getDescription() + "\n\n" + content + "\n\n" +
                        ctx.translate(Strings.APP, "usage_item_add", ctx.getPrefixUsed() +
                                String.join(" ", ctx.input().getTrigger())) + "\n" +
                        ctx.translate(Strings.APP, "usage_item_remove", ctx.getPrefixUsed() +
                                String.join(" ", ctx.input().getTrigger()))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Command> subcommands() {
        return Set.of(
                Command.builder("add", ctx -> grammar.resolve(ctx)
                        .flatMap(args -> add(ctx, (E) args.item)
                                .then(ctx.channel()
                                        .createMessage(ctx.translate(Strings.APP, "item_add_success",
                                                formatItem((E) args.item)))
                                        .then())))
                        .inheritFrom(this)
                        .build(),
                Command.builder("remove", ctx -> grammar.resolve(ctx)
                        .flatMap(args -> remove(ctx, (E) args.item)
                                .then(ctx.channel()
                                        .createMessage(ctx.translate(Strings.APP, "item_remove_success",
                                                formatItem((E) args.item)))
                                        .then())))
                        .inheritFrom(this)
                        .build());
    }

    private final static class Args {
        Object item;
    }
}
