package ultimategdbot.service;

import botrino.api.i18n.Translator;
import jdash.client.GDClient;
import jdash.client.request.GDRequests;
import jdash.client.request.GDRouter;
import jdash.common.DemonDifficulty;
import jdash.common.Difficulty;
import jdash.common.Length;
import jdash.common.QualityRating;
import jdash.common.entity.GDLevel;
import jdash.common.entity.GDSong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ultimategdbot.util.EmbedType;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GDLevelServiceTest {

    // From jdash test resources — getSongInfo.txt
    private static final String GET_SONG_INFO_RESPONSE =
            "1~|~844899~|~2~|~~:Space soup:~~|~3~|~28916~|~4~|~lchavasse~|~5~|~8.79~|~6~|~~|~10~|~" +
                    "https%3A%2F%2Faudio.ngfiles.com%2F844000%2F844899_Space-soup.mp3%3Ff1548488779~|~7~|~";
    private final Translator translator = Translator.to(Locale.ENGLISH);
    @Mock
    private EmojiService emojiService;

    private static GDLevel levelWithOfficialSong() {
        return new GDLevel(
                10565740L, "Bloodbath", 503085L, "Test description",
                Difficulty.INSANE, DemonDifficulty.EXTREME,
                10, 50, QualityRating.EPIC,
                26672952, 1505455, Length.XL,
                3, true, 1, 21, 24746,
                true, false,
                Optional.empty(), 0,
                Optional.empty(),
                Optional.of(GDSong.getOfficialSong(0).orElseThrow()),
                Optional.of("Riot"),
                Optional.of(503085L),
                false, false
        );
    }

    private static GDLevel levelWithSongId(long songId) {
        return new GDLevel(
                123456L, "Test Level", 100L, "",
                Difficulty.HARD, DemonDifficulty.HARD,
                3, 0, QualityRating.NONE,
                1000, 200, Length.SHORT,
                0, false, 1, 22, 500,
                false, false,
                Optional.empty(), 0,
                Optional.of(songId),
                Optional.empty(),
                Optional.of("Creator"),
                Optional.empty(),
                false, false
        );
    }

    private static GDLevel levelWithNoSong() {
        return new GDLevel(
                789L, "No Song Level", 42L, "",
                Difficulty.NA, DemonDifficulty.HARD,
                0, 0, QualityRating.NONE,
                500, 100, Length.TINY,
                0, false, 1, 22, 100,
                false, false,
                Optional.empty(), 0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false, false
        );
    }

    @BeforeEach
    void setUp() {
        when(emojiService.get(anyString())).thenAnswer(inv -> "[" + inv.getArgument(0) + "]");
    }

    private GDLevelService serviceWithRouter(GDRouter router) {
        return new GDLevelService(emojiService, GDClient.create().withRouter(router));
    }

    @Test
    void compactEmbed_levelWithOfficialSong_embedFieldContainsLevelName() {
        var service = serviceWithRouter(request ->
                Mono.error(new RuntimeException("Unexpected request: " + request.getUri())));

        StepVerifier.create(service.compactEmbed(translator, levelWithOfficialSong(), EmbedType.LEVEL_SEARCH_RESULT,
                        null))
                .assertNext(tuple -> {
                    var embed = tuple.getT1();
                    var firstField = embed.fields().get(0);
                    assertTrue(firstField.name().contains("Bloodbath"),
                            "First field name should contain level name");
                    assertTrue(firstField.name().contains("Riot"),
                            "First field name should contain creator name");
                })
                .verifyComplete();
    }

    @Test
    void compactEmbed_levelWithOfficialSong_footerContainsLevelId() {
        var service = serviceWithRouter(request ->
                Mono.error(new RuntimeException("Unexpected request: " + request.getUri())));

        StepVerifier.create(service.compactEmbed(translator, levelWithOfficialSong(), EmbedType.LEVEL_SEARCH_RESULT,
                        null))
                .assertNext(tuple -> assertTrue(
                        tuple.getT1().toString().contains("10565740"),
                        "Embed spec should contain level ID in footer"))
                .verifyComplete();
    }

    @Test
    void compactEmbed_levelWithOfficialSong_providesTwoFiles() {
        var service = serviceWithRouter(request ->
                Mono.error(new RuntimeException("Unexpected request: " + request.getUri())));

        StepVerifier.create(service.compactEmbed(translator, levelWithOfficialSong(), EmbedType.LEVEL_SEARCH_RESULT,
                        null))
                .assertNext(tuple -> assertEquals(2, tuple.getT2().size(),
                        "Should provide exactly 2 files (difficulty.png and author.png)"))
                .verifyComplete();
    }

    @Test
    void compactEmbed_levelWithOfficialSong_secondFieldContainsSongTitle() {
        var service = serviceWithRouter(request ->
                Mono.error(new RuntimeException("Unexpected request: " + request.getUri())));

        StepVerifier.create(service.compactEmbed(translator, levelWithOfficialSong(), EmbedType.LEVEL_SEARCH_RESULT,
                        null))
                .assertNext(tuple -> {
                    var secondField = tuple.getT1().fields().get(1);
                    // Official song "Stereo Madness" — value contains song info
                    assertTrue(secondField.value().contains("Stereo Madness"),
                            "Second field value should contain the official song title");
                })
                .verifyComplete();
    }

    @Test
    void compactEmbed_levelWithNewgroundsSongId_fetchesSongFromClientAndIncludesTitle() {
        GDRouter router = request -> switch (request.getUri()) {
            case GDRequests.GET_GJ_SONG_INFO -> Mono.just(GET_SONG_INFO_RESPONSE);
            default -> Mono.error(new RuntimeException("Unexpected request: " + request.getUri()));
        };
        var service = serviceWithRouter(router);

        StepVerifier.create(service.compactEmbed(translator, levelWithSongId(844899L), EmbedType.LEVEL_SEARCH_RESULT,
                        null))
                .assertNext(tuple -> {
                    var secondField = tuple.getT1().fields().get(1);
                    assertTrue(secondField.value().contains("Space soup"),
                            "Second field value should contain the fetched Newgrounds song title");
                })
                .verifyComplete();
    }

    @Test
    void compactEmbed_levelWithNoSong_secondFieldShowsUnknownSong() {
        var service = serviceWithRouter(request ->
                Mono.error(new RuntimeException("Unexpected request: " + request.getUri())));

        StepVerifier.create(service.compactEmbed(translator, levelWithNoSong(), EmbedType.LEVEL_SEARCH_RESULT, null))
                .assertNext(tuple -> {
                    var secondField = tuple.getT1().fields().get(1);
                    // The "unknown song" fallback includes a warning emoji
                    assertTrue(secondField.value().contains(":warning:"),
                            "Second field value should show the unknown song warning");
                })
                .verifyComplete();
    }

    @Test
    void compactEmbed_withDailyEmbedType_embedSpecContainsDailyAuthorName() {
        var service = serviceWithRouter(request ->
                Mono.error(new RuntimeException("Unexpected request: " + request.getUri())));
        // EmbedType.DAILY_LEVEL uses the "daily" translation key for the author name
        var expectedAuthorName = EmbedType.DAILY_LEVEL.getAuthorName(translator);

        StepVerifier.create(service.compactEmbed(translator, levelWithOfficialSong(), EmbedType.DAILY_LEVEL, null))
                .assertNext(tuple -> assertTrue(
                        tuple.getT1().toString().contains(expectedAuthorName),
                        "Embed spec should contain the daily author name"))
                .verifyComplete();
    }

    @Test
    void compactEmbed_noTimelyInfo_authorNameHasNoNumberSuffix() {
        var service = serviceWithRouter(request ->
                Mono.error(new RuntimeException("Unexpected request: " + request.getUri())));

        StepVerifier.create(service.compactEmbed(translator, levelWithOfficialSong(), EmbedType.LEVEL_SEARCH_RESULT,
                        null))
                .assertNext(tuple -> assertFalse(
                        tuple.getT1().toString().contains(" #"),
                        "Embed spec should not contain '#' suffix when timelyInfo is null"))
                .verifyComplete();
    }
}
