package ultimategdbot.command;

import botrino.api.annotation.Exclude;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.reactivestreams.Publisher;

import java.util.List;

@Exclude
public abstract class CommandWithOptions<O> implements ChatInputInteractionListener {

    private final ChatInputCommandGrammar<O> grammar = ChatInputCommandGrammar.of(optionClass());

    @Override
    public List<ApplicationCommandOptionData> options() {
        return grammar.toOptions();
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return grammar.resolve(ctx.event()).flatMapMany(options -> runWithOptions(ctx, options));
    }

    protected abstract Class<O> optionClass();
    protected abstract Publisher<?> runWithOptions(ChatInputInteractionContext ctx, O options);
}
