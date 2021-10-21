package ultimategdbot.command;

import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.common.Role;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.database.ImmutableGdMod;
import ultimategdbot.event.ImmutableModStatusUpdate;
import ultimategdbot.event.ManualEventProducer;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.service.PrivilegeFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ultimategdbot.event.ModStatusUpdate.Type.*;

@RdiService
@ChatInputCommand(name = "mod-wave", description = "Trigger moderator promotion/demotion events (Elder Mod only).",
        defaultPermission = false)
public final class ModWaveCommand implements ChatInputInteractionListener {

    private final DatabaseService db;
    private final EmojiService emoji;
    private final GDUserService userService;
    private final ManualEventProducer eventProducer;
    private final PrivilegeFactory privilegeFactory;

    @RdiFactory
    public ModWaveCommand(DatabaseService db, EmojiService emoji, GDUserService userService,
                          ManualEventProducer eventProducer, PrivilegeFactory privilegeFactory) {
        this.db = db;
        this.emoji = emoji;
        this.userService = userService;
        this.eventProducer = eventProducer;
        this.privilegeFactory = privilegeFactory;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return Flux.fromIterable(ctx.event().getOptions())
                .flatMap(option -> Mono.justOrEmpty(option.getValue())
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .flatMap(value -> userService.stringToUser(ctx, value)))
                .flatMap(user ->
                        ctx.event()
                        .createFollowup(ctx.translate(Strings.GD, "checking_mod", user.name()) + "\n||" +
                                (user.role().orElse(Role.USER) == Role.USER
                                ? emoji.get("failed") + ' ' + ctx.translate(Strings.GD, "checkmod_failed")
                                : emoji.get("success") + ' ' + ctx.translate(Strings.GD, "checkmod_success",
                                        user.role().orElseThrow())) + "||")
                        .then(db.gdModDao().get(user.accountId()))
                        .switchIfEmpty(Mono.defer(() -> {
                            if (user.role().map(Role.USER::equals).orElse(true)) {
                                return Mono.empty();
                            }
                            final var isElder = user.role().map(Role.ELDER_MODERATOR::equals).orElse(false);
                            eventProducer.submit(ImmutableModStatusUpdate.of(user, isElder
                                    ? PROMOTED_TO_ELDER : PROMOTED_TO_MOD));
                            return db.gdModDao()
                                    .save(ImmutableGdMod.builder()
                                            .accountId(user.accountId())
                                            .name(user.name())
                                            .elder(isElder ? 1 : 0)
                                            .build())
                                    .then(Mono.empty());
                        }))
                        .flatMap(gdMod -> {
                            if (user.role().map(Role.USER::equals).orElse(true)) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(user, gdMod.isElder()
                                        ? DEMOTED_FROM_ELDER : DEMOTED_FROM_MOD));
                                return db.gdModDao().delete(gdMod.accountId());
                            }
                            final var newGdMod = ImmutableGdMod.builder().from(gdMod);
                            if(user.role().map(Role.MODERATOR::equals).orElse(false) && gdMod.isElder()) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(user, DEMOTED_FROM_ELDER));
                                newGdMod.elder(0);
                            } else if (user.role().map(Role.ELDER_MODERATOR::equals).orElse(false) && !gdMod.isElder()) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(user, PROMOTED_TO_ELDER));
                                newGdMod.elder(1);
                            }
                            newGdMod.name(user.name());
                            return db.gdModDao().save(newGdMod.build());
                        })
                )
                .then();
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return IntStream.range(0, 10)
                .mapToObj(i -> ApplicationCommandOptionData.builder()
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .name("gd-username-" + (i + 1))
                        .description("The GD username of a user in the wave. (" + (i + 1) + ")")
                        .required(i == 0)
                        .build())
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.elderMod();
    }
}
