package ultimategdbot.command;

import botrino.api.config.ConfigContainer;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.config.InteractionConfig;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.privilege.Privilege;
import botrino.interaction.privilege.Privileges;
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
import ultimategdbot.database.GdModDao;
import ultimategdbot.database.ImmutableGdMod;
import ultimategdbot.event.ImmutableModStatusUpdate;
import ultimategdbot.event.ManualEventProducer;
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

    private final GdModDao gdModDao;
    private final EmojiService emoji;
    private final GDUserService userService;
    private final ManualEventProducer eventProducer;
    private final PrivilegeFactory privilegeFactory;
    private final ConfigContainer configContainer;

    @RdiFactory
    public ModWaveCommand(GdModDao gdModDao, EmojiService emoji, GDUserService userService,
                          ManualEventProducer eventProducer, PrivilegeFactory privilegeFactory,
                          ConfigContainer configContainer) {
        this.gdModDao = gdModDao;
        this.emoji = emoji;
        this.userService = userService;
        this.eventProducer = eventProducer;
        this.privilegeFactory = privilegeFactory;
        this.configContainer = configContainer;
    }

    @Override
    @SuppressWarnings("ReactiveStreamsThrowInOperator")
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
                        .then(gdModDao.get(profile.user().accountId()))
                        .switchIfEmpty(Mono.defer(() -> {
                            if (profile.user().role().map(Role.USER::equals).orElse(true)) {
                                return Mono.empty();
                            }
                            final var elder = profile.user().role().orElseThrow().ordinal() - 1;
                            eventProducer.submit(ImmutableModStatusUpdate.of(profile, switch (elder) {
                                case 0 -> PROMOTED_TO_MOD;
                                case 1 -> PROMOTED_TO_ELDER;
                                case 2 -> PROMOTED_TO_LBMOD;
                                default -> throw new AssertionError();
                            }));
                            return gdModDao
                                    .save(ImmutableGdMod.builder()
                                            .accountId(profile.user().accountId())
                                            .name(profile.user().name())
                                            .elder(elder)
                                            .build())
                                    .then(Mono.empty());
                        }))
                        .flatMap(gdMod -> {
                            if (profile.user().role().map(Role.USER::equals).orElse(true)) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(profile, switch (gdMod.elder()) {
                                    case 0 -> DEMOTED_FROM_MOD;
                                    case 1 -> DEMOTED_FROM_ELDER;
                                    case 2 -> DEMOTED_FROM_LBMOD;
                                    default -> throw new AssertionError();
                                }));
                                return gdModDao.delete(gdMod.accountId());
                            }
                            final var newGdMod = ImmutableGdMod.builder().from(gdMod);
                            switch (profile.user().role().orElseThrow()) {
                                case MODERATOR -> {
                                    final var type = switch (gdMod.elder()) {
                                        case 1 -> DEMOTED_FROM_ELDER;
                                        case 2 -> PROMOTED_TO_MOD;
                                        default -> null;
                                    };
                                    if (type != null) {
                                        eventProducer.submit(ImmutableModStatusUpdate.of(profile, type));
                                    }
                                    newGdMod.elder(0);
                                }
                                case ELDER_MODERATOR -> {
                                    final var type = switch (gdMod.elder()) {
                                        case 0, 2 -> PROMOTED_TO_ELDER;
                                        default -> null;
                                    };
                                    if (type != null) {
                                        eventProducer.submit(ImmutableModStatusUpdate.of(profile, type));
                                    }
                                    newGdMod.elder(1);
                                }
                                case LEADERBOARD_MODERATOR -> {
                                    final var type = switch (gdMod.elder()) {
                                        case 0, 1 -> PROMOTED_TO_LBMOD;
                                        default -> null;
                                    };
                                    if (type != null) {
                                        eventProducer.submit(ImmutableModStatusUpdate.of(profile, type));
                                    }
                                    newGdMod.elder(2);
                                }
                            }
                            newGdMod.name(profile.user().name());
                            return gdModDao.save(newGdMod.build());
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
        // Allow command everywhere if dev/beta, else only allow elder mods
        return configContainer.get(InteractionConfig.class).applicationCommandsGuildId()
                .map(__ -> Privileges.allowed())
                .orElse(privilegeFactory.elderMod());
    }
}
