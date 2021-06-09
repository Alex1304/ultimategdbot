package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.Markdown;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.database.GdMod;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.OutputPaginator;

import java.util.Comparator;
import java.util.stream.Collectors;

@CommandCategory(CommandCategory.GD)
@Alias("modlist")
@TopLevelCommand
@RdiService
public final class ModListCommand implements Command {

    private final EmojiService emoji;
    private final DatabaseService db;
    private final OutputPaginator paginator;

    @RdiFactory
    public ModListCommand(EmojiService emoji, DatabaseService db, OutputPaginator paginator) {
        this.emoji = emoji;
        this.db = db;
        this.paginator = paginator;
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return db.gdModDao().getAll().collectList()
                .flatMap(modList -> paginator.paginate(ctx,
                        modList.stream()
                                .sorted(Comparator.comparingInt(GdMod::elder).reversed()
                                        .thenComparing(GdMod::name, String.CASE_INSENSITIVE_ORDER))
                                .map(gdMod -> (gdMod.isElder() ? emoji.get("elder_mod") : emoji.get("mod")) + ' ' +
                                        Markdown.bold(gdMod.name()))
                                .collect(Collectors.toList()),
                        content -> "**__" + ctx.translate(Strings.GD, "mod_list") + "__\n**" +
                                ctx.translate(Strings.GD, "modlist_intro", ctx.getPrefixUsed()) + "\n\n" + content));
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.HELP, "modlist_description"))
                .build();
    }
}
