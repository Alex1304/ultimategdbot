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
import discord4j.rest.util.Permission;
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
import java.util.stream.IntStream;

import static ultimategdbot.event.ModStatusUpdate.Type.*;

@RdiService
@ChatInputCommand(name = "mod-wave", description = "Trigger moderator promotion/demotion events (Elder Mod only).",
        defaultMemberPermissions = Permission.ADMINISTRATOR)
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
                .flatMap(profile ->
                        ctx.event()
                        .createFollowup(ctx.translate(Strings.GD, "checking_mod", profile.user().name()) + "\n||" +
                                (profile.user().role().orElse(Role.USER) == Role.USER
                                ? emoji.get("failed") + ' ' + ctx.translate(Strings.GD, "checkmod_failed")
                                : emoji.get("success") + ' ' + ctx.translate(Strings.GD, "checkmod_success",
                                        profile.user().role().orElseThrow())) + "||")
                        .then(db.gdModDao().get(profile.user().accountId()))
                        .switchIfEmpty(Mono.defer(() -> {
                            if (profile.user().role().map(Role.USER::equals).orElse(true)) {
                                return Mono.empty();
                            }
                            final var isElder = profile.user().role().map(Role.ELDER_MODERATOR::equals).orElse(false);
                            eventProducer.submit(ImmutableModStatusUpdate.of(profile, isElder
                                    ? PROMOTED_TO_ELDER : PROMOTED_TO_MOD));
                            return db.gdModDao()
                                    .save(ImmutableGdMod.builder()
                                            .accountId(profile.user().accountId())
                                            .name(profile.user().name())
                                            .elder(isElder ? 1 : 0)
                                            .build())
                                    .then(Mono.empty());
                        }))
                        .flatMap(gdMod -> {
                            if (profile.user().role().map(Role.USER::equals).orElse(true)) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(profile, gdMod.isElder()
                                        ? DEMOTED_FROM_ELDER : DEMOTED_FROM_MOD));
                                return db.gdModDao().delete(gdMod.accountId());
                            }
                            final var newGdMod = ImmutableGdMod.builder().from(gdMod);
                            if (profile.user().role().map(Role.MODERATOR::equals).orElse(false) && gdMod.isElder()) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(profile, DEMOTED_FROM_ELDER));
                                newGdMod.elder(0);
                            } else if (profile.user().role().map(Role.ELDER_MODERATOR::equals).orElse(false) && !gdMod.isElder()) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(profile, PROMOTED_TO_ELDER));
                                newGdMod.elder(1);
                            }
                            newGdMod.name(profile.user().name());
                            return db.gdModDao().save(newGdMod.build());
                        })
                )
                .then();
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return IntStream.range(0, 10)
                .<ApplicationCommandOptionData>mapToObj(i -> ApplicationCommandOptionData.builder()
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .name("gd-username-" + (i + 1))
                        .description("The GD username of a user in the wave. (" + (i + 1) + ")")
                        .required(i == 0)
                        .build())
                .toList();
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.elderMod();
    }
}
