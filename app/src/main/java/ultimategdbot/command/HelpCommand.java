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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

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
            sb.append("__Flags:__\n");
            for (var flagInfo : doc.getFlags()) {
                sb.append('`');
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
            sb.append("__Subcommands:__\n");
            sb.append(subs);
        }
        return outputPaginator.paginate(ctx, sb.toString().lines().collect(Collectors.toList()));
    }

    private static String formatCommandEntry(Command cmd, CommandContext ctx, List<String> parentAliases) {
        var aliases = cmd.aliases().stream().sorted().collect(joining("|"));
        var desc = Optional.of(cmd.documentation(ctx).getDescription())
                .filter(not(String::isEmpty))
                .orElseGet(() -> Markdown.italic("No description"));
        var parents = (String.join(" ", parentAliases) + " ").strip();
        return Markdown.code(ctx.getPrefixUsed() + parents + aliases) + ": " + desc;
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return grammar.resolve(ctx).flatMap(args -> {
            if (args.command.isEmpty()) {
                // List all top-level commands
                return outputPaginator.paginate(ctx,
                        commandService.listCommands().stream()
                                .map(cmd -> formatCommandEntry(cmd, ctx, List.of()))
                                .sorted()
                                .collect(Collectors.toList()),
                        content -> ctx.translate(Strings.APP, "help_intro", ctx.getPrefixUsed()) + "\n\n" + content);
            }
            // Send documentation for specific command
            var alias = args.command.get(0);
            var subcommands = args.command.subList(1, args.command.size());
            var cmdFound = commandService.getCommandAt(alias, subcommands.toArray(new String[0]));
            return cmdFound.map(cmd -> buildMessage(cmd, ctx, alias, subcommands))
                    .orElseGet(() -> Mono.error(new CommandFailedException("Command not found")));
        }).then();
    }

    @Override
    public CommandDocumentation documentation(Translator translator) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription("Displays helpful info on commands.")
                .setBody("Without arguments, gives a list of available commands. Pass a command or a sequence " +
                        "of subcommands in arguments to get detailed information on that specific command/subcommand.")
                .build();
    }

    private static final class Args {
        List<String> command;
    }
}
