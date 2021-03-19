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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Alias("help")
@TopLevelCommand
@RdiService
public final class HelpCommand implements Command {

    private final CommandGrammar<Args> grammar = CommandGrammar.builder()
            .nextArgument("command")
            .setVarargs(true)
            .build(Args.class);

    private final CommandService commandService;

    @RdiFactory
    public HelpCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    private static String formatDocForCommand(Command cmd, CommandContext ctx, Args args) {
        var doc = cmd.documentation(ctx);
        var sb = new StringBuilder();
        sb.append("```\n");
        sb.append(ctx.getPrefixUsed());
        sb.append(String.join(" ", args.command));
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
        return sb.toString();
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return grammar.resolve(ctx).flatMap(args -> {
            if (args.command.isEmpty()) {
                // List all top-level commands
                return ctx.channel().createMessage(commandService.listCommands().stream()
                        .map(cmd -> {
                            var aliases = String.join("|", cmd.aliases());
                            var desc = Optional.of(cmd.documentation(ctx).getDescription())
                                    .filter(not(String::isEmpty))
                                    .orElseGet(() -> Markdown.italic("No description"));
                            return Markdown.code(ctx.getPrefixUsed() + aliases) + ": " + desc;
                        })
                        .collect(Collectors.joining("\n")));
            }
            // Send documentation for specific command
            var cmdFound = commandService.getCommandAt(args.command.get(0),
                    args.command.subList(1, args.command.size()).toArray(new String[0]));
            return cmdFound.map(cmd -> ctx.channel().createMessage(formatDocForCommand(cmd, ctx, args)))
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
        private List<String> command;
    }
}
