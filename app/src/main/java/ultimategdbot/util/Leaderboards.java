package ultimategdbot.util;

import botrino.api.i18n.Translator;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLeaderboard;

import java.util.List;
import java.util.Objects;

public final class Leaderboards {

    public static final int ENTRIES_PER_PAGE = 20;

    private Leaderboards() {
        throw new AssertionError();
    }

    public static EmbedCreateSpec embed(Translator tr, Guild guild, List<LeaderboardEntry> entryList,
                                        int page, @Nullable String highlighted, String emoji) {
        final var size = entryList.size();
        final var maxPage = (size - 1) / ENTRIES_PER_PAGE;
        final var offset = page * ENTRIES_PER_PAGE;
        final var subList = entryList.subList(offset, Math.min(offset + ENTRIES_PER_PAGE, size));
        var embed = EmbedCreateSpec.builder()
                .title(tr.translate(Strings.GD, "lb_title", guild.getName()));
        if (size == 0 || subList.isEmpty()) {
            return embed.description(tr.translate(Strings.GD, "lb_no_entries")).build();
        }
        var sb = new StringBuilder();
        var rankWidth = (int) Math.log10(size) + 1;
        var statWidth = (int) Math.log10(subList.get(0).getValue()) + 1;
        final var maxRowLength = 100;
        for (var i = 1; i <= subList.size(); i++) {
            var entry = subList.get(i - 1);
            var isHighlighted = entry.getStats().name().equalsIgnoreCase(highlighted);
            var rank = page * ENTRIES_PER_PAGE + i;
            if (isHighlighted) {
                sb.append("**");
            }
            var row = String.format("%s | %s %s | %s (%s)",
                    String.format("`#%" + rankWidth + "d`", rank).replaceAll(" ", " ‌‌"),
                    emoji,
                    GDFormatter.formatCode(entry.getValue(), statWidth),
                    entry.getStats().name(),
                    entry.getDiscordUser());
            if (row.length() > maxRowLength) {
                row = row.substring(0, maxRowLength - 3) + "...";
            }
            sb.append(row).append("\n");
            if (isHighlighted) {
                sb.append("**");
            }
        }
        embed.description("**" + tr.translate(Strings.GD, "lb_total_players", size, emoji) + "**\n\n" + sb);
        embed.addField("───────────", tr.translate(Strings.GD, "lb_account_notice"), false);
        if (maxPage > 0) {
            embed.addField(tr.translate(Strings.GENERAL, "page_x", page + 1, maxPage + 1),
                    tr.translate(Strings.GENERAL, "page_instructions"), false);
        }
        return embed.build();
    }

    public static class LeaderboardEntry implements Comparable<LeaderboardEntry> {
        private final int value;
        private final GdLeaderboard stats;
        private final String discordUser;

        public LeaderboardEntry(int value, GdLeaderboard stats, String discordUser) {
            this.value = value;
            this.stats = Objects.requireNonNull(stats);
            this.discordUser = Objects.requireNonNull(discordUser);
        }

        public int getValue() {
            return value;
        }

        public GdLeaderboard getStats() {
            return stats;
        }

        public String getDiscordUser() {
            return discordUser;
        }

        @Override
        public int compareTo(LeaderboardEntry o) {
            return value == o.value ? stats.name().compareToIgnoreCase(o.stats.name()) : o.value - value;
        }

        @Override
        public String toString() {
            return "LeaderboardEntry{" + stats.name() + ": " + value + "}";
        }
    }
}
