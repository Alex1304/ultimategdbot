package ultimategdbot.service;

import botrino.api.i18n.Translator;
import botrino.interaction.InteractionFailedException;
import jdash.client.GDClient;
import jdash.client.request.GDRequests;
import jdash.client.request.GDRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GDUserServiceTest {

    // Taken from jdash test resources
    private static final String SEARCH_USERS_RESPONSE =
            "1:Alex1304:2:4063664:13:100:17:545:6::9:29:52:0:10:12:11:9:14:0:15:2:16:98006:3:3411:8:21:4:23#999:0:10";
    private static final String GET_USER_PROFILE_RESPONSE =
            "1:Alex1304:2:4063664:13:100:17:818:10:12:11:9:51:9:3:5658:52:0:46:19336:4:46:8:21:18:0:19:0:50:0:20:" +
            "UC0hFAVN-GAbZYuf_Hfk1Iog:21:29:22:7:23:30:24:3:25:24:26:21:28:1:43:15:48:15:53:22:54:1:30:33266:" +
            "16:98006:31:0:44:gd_alex1304:45:gd_alex1304:49:1:38:0:39:0:40:0:29:1";

    private GDUserService service;
    private final Translator translator = Translator.to(Locale.ENGLISH);

    @BeforeEach
    void setUp() {
        GDRouter router = request -> switch (request.getUri()) {
            case GDRequests.GET_GJ_USERS_20 -> Mono.just(SEARCH_USERS_RESPONSE);
            case GDRequests.GET_GJ_USER_INFO_20 -> Mono.just(GET_USER_PROFILE_RESPONSE);
            default -> Mono.error(new RuntimeException("Unexpected request: " + request.getUri()));
        };
        GDClient gdClient = GDClient.create().withRouter(router);
        service = new GDUserService(null, null, null, gdClient, null);
    }

    @Test
    void stringToUser_validUsername_returnsUserProfile() {
        StepVerifier.create(service.stringToUser(translator, "Alex1304"))
                .assertNext(profile -> {
                    assertEquals("Alex1304", profile.user().name());
                    assertEquals(98006, profile.user().accountId());
                })
                .verifyComplete();
    }

    @Test
    void stringToUser_invalidCharacters_failsWithInteractionFailed() {
        StepVerifier.create(service.stringToUser(translator, "Alex!@#$"))
                .expectError(InteractionFailedException.class)
                .verify();
    }

    @Test
    void stringToUser_userWithNoAccount_returnsEmpty() {
        // accountId = 0 means unregistered account — should be filtered out
        GDRouter router = request -> Mono.just(
                "1:UnregisteredUser:2:12345:13:0:17:0:6::9:0:52:0:10:0:11:0:14:0:15:0:16:0:3:0:8:0:4:0#1:0:0");
        GDClient gdClient = GDClient.create().withRouter(router);
        service = new GDUserService(null, null, null, gdClient, null);

        StepVerifier.create(service.stringToUser(translator, "UnregisteredUser"))
                .verifyComplete();
    }
}
