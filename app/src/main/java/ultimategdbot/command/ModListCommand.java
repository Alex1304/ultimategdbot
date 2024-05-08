package ultimategdbot.command;

import botrino.api.util.Markdown;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import org.reactivestreams.Publisher;
import ultimategdbot.Strings;
import ultimategdbot.database.GdMod;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.OutputPaginator;

import java.util.Comparator;

@RdiService
@ChatInputCommand(name = "mod-list", description = "Displays the full list of last known Geometry Dash moderators.")
public final class ModListCommand implements ChatInputInteractionListener {

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
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return db.gdModDao().getAll().collectList()
                .flatMap(modList -> paginator.paginate(ctx,
                        modList.stream()
                                .sorted(Comparator.comparingInt(GdMod::elder).reversed()
                                        .thenComparing(GdMod::name, String.CASE_INSENSITIVE_ORDER))
                                .map(gdMod -> (gdMod.isElder() ? emoji.get("elder_mod") : emoji.get("mod")) + ' ' +
                                        Markdown.bold(gdMod.name()))
                                .toList(),
                        content -> "**__" + ctx.translate(Strings.GD, "mod_list") + "__\n**" +
                                ctx.translate(Strings.GD, "modlist_intro") + "\n\n" + content));
    }
}
