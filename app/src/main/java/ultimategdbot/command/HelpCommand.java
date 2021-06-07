package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.Markdown;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.CommandService;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.grammar.CommandGrammar;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.OutputPaginator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

@CommandCategory(CommandCategory.GENERAL)
@Alias({"help", "manual", "commands"})
@TopLevelCommand
@RdiService
public final class HelpCommand implements Command {

    private final CommandGrammar<Args> grammar = CommandGrammar.builder()
            .nextArgument("command")
            .setVarargs(true)
            .build(Args.class);

    private final CommandService commandService;
    private final OutputPaginator outputPaginator;

    @RdiFactory
    public HelpCommand(CommandService commandService, OutputPaginator outputPaginator) {
        this.commandService = commandService;
        this.outputPaginator = outputPaginator;
    }

    private static String formatCommandEntry(Command cmd, CommandContext ctx, List<String> parentAliases) {
        var aliases = cmd.aliases().stream().sorted().collect(joining("|"));
        var desc = Optional.of(cmd.documentation(ctx).getDescription())
                .filter(not(String::isEmpty))
                .orElseGet(() -> Markdown.italic(ctx.translate(Strings.APP, "no_description")));
        var aliasSeq = Stream.concat(parentAliases.stream(), Stream.of(aliases))
                .collect(Collectors.joining(" "));
        return Markdown.code(ctx.getPrefixUsed() + aliasSeq) + ": " + desc;
    }

    private Mono<Void> buildMessage(Command cmd, CommandContext ctx, String alias, List<String> subcommands) {
        var doc = cmd.documentation(ctx);
        var sb = new StringBuilder();
        sb.append("```\n");
        sb.append(ctx.getPrefixUsed());
        sb.append(alias);
        if (!subcommands.isEmpty()) {
            sb.append(' ');
            sb.append(String.join(" ", subcommands));
        }
        sb.append(' ');
        sb.append(doc.getSyntax());
        sb.append("\n```\n");
        sb.append(doc.getDescription());
        sb.append("\n\n");
        sb.append(doc.getBody());
        sb.append('\n');
        if (!doc.getFlags().isEmpty()) {
            sb.append("\n__**").append(ctx.translate(Strings.APP, "flags")).append("**__\n");
            for (var flagInfo : doc.getFlags()) {
                sb.append("`-");
                sb.append(flagInfo.getValueFormat());
                sb.append("`: ");
                sb.append(flagInfo.getDescription());
                sb.append('\n');
            }
        }
        var aliasSeq = new ArrayList<String>();
        aliasSeq.add(alias);
        aliasSeq.addAll(subcommands);
        var subs = commandService.listCommands(aliasSeq.toArray(String[]::new))
                .stream()
                .map(sub -> formatCommandEntry(sub, ctx, aliasSeq))
                .sorted()
                .collect(joining("\n"));
        if (!subs.isBlank()) {
            sb.append("\n__**").append(ctx.translate(Strings.APP, "subcommands")).append("**__\n");
            sb.append(subs);
        }
        return outputPaginator.paginate(ctx, sb.toString().lines().collect(Collectors.toList()));
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return grammar.resolve(ctx).flatMap(args -> {
            if (args.command.isEmpty()) {
                // List all top-level commands
                return outputPaginator.paginate(ctx,
                        commandService.listCommands().stream()
                                .collect(Collectors.groupingBy(cmd -> {
                                    final var category = cmd.getClass().getAnnotation(CommandCategory.class);
                                    return category != null ? category.value()
                                            : ctx.translate(Strings.APP, "unknown");
                                }))
                                .entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByKey())
                                .flatMap(entry -> Stream.concat(Stream.of("\n**__" + entry.getKey() + "__**"),
                                        entry.getValue().stream()
                                                .map(cmd -> formatCommandEntry(cmd, ctx, List.of()))
                                                .sorted()))
                                .collect(Collectors.toList()),
                        content -> ctx.translate(Strings.APP, "help_intro", ctx.getPrefixUsed()) + "\n" + content);
            }
            // Send documentation for specific command
            var alias = args.command.get(0);
            var subcommands = args.command.subList(1, args.command.size());
            var cmdFound = commandService.getCommandAt(alias, subcommands.toArray(new String[0]));
            return cmdFound.map(cmd -> buildMessage(cmd, ctx, alias, subcommands))
                    .orElseGet(() -> Mono.error(new CommandFailedException(
                            ctx.translate(Strings.APP, "command_not_found"))));
        }).then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription(tr.translate(Strings.HELP, "help_description"))
                .setBody(tr.translate(Strings.HELP, "help_body"))
                .build();
    }

    private static final class Args {
        List<String> command;
    }
}
