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
            .nextArgument("element", argumentMapper())
            .build(Args.class);

    AddRemoveListCommand(OutputPaginator outputPaginator) {
        this.outputPaginator = outputPaginator;
    }

    abstract ArgumentMapper<E> argumentMapper();

    abstract Mono<Void> add(CommandContext ctx, E element);

    abstract Mono<Void> remove(CommandContext ctx, E element);

    abstract Flux<String> listFormattedItems(CommandContext ctx);

    abstract String syntax();

    String formatElement(E element) {
        return String.valueOf(element);
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        var docs = documentation(ctx);
        return listFormattedItems(ctx).collectList().flatMap(list -> outputPaginator.paginate(ctx, list,
                content -> docs.getDescription() + "\n\n" + content + "\n\n" +
                        ctx.translate(Strings.APP, "usage_element_add", ctx.getPrefixUsed() + syntax()) +
                        "\n" +
                        ctx.translate(Strings.APP, "usage_element_remove", ctx.getPrefixUsed() + syntax())));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Command> subcommands() {
        return Set.of(
                Command.builder("add", ctx -> grammar.resolve(ctx)
                        .flatMap(args -> add(ctx, (E) args.element)
                                .then(ctx.channel()
                                        .createMessage(ctx.translate(Strings.APP, "element_add_success",
                                                formatElement((E) args.element)))
                                        .then())))
                        .inheritFrom(this)
                        .build(),
                Command.builder("remove", ctx -> grammar.resolve(ctx)
                        .flatMap(args -> remove(ctx, (E) args.element)
                                .then(ctx.channel()
                                        .createMessage(ctx.translate(Strings.APP, "element_remove_success",
                                                formatElement((E) args.element)))
                                        .then())))
                        .inheritFrom(this)
                        .build());
    }

    private final static class Args {
        private Object element;
    }
}
