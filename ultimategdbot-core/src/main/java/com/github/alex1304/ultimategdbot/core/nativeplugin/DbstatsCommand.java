package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.core.impl.DatabaseImpl;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class DbstatsCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		if (!(ctx.getBot().getDatabase() instanceof DatabaseImpl)) {
			return Mono.error(new CommandFailedException("Sadly, the database API this bot is using does not support viewing statistics."));
		}
		var db = (DatabaseImpl) ctx.getBot().getDatabase();
		var stats = db.getSessionFactory().getStatistics();
		var sb = new StringBuilder("**__Database usage statistics__**\n\n");
		sb.append("**Entity fetch count:** ").append(stats.getEntityFetchCount()).append('\n');
		sb.append("**Entity load count:** ").append(stats.getEntityLoadCount()).append('\n');
		sb.append("**Entity insert count:** ").append(stats.getEntityInsertCount()).append('\n');
		sb.append("**Entity update count:** ").append(stats.getEntityUpdateCount()).append('\n');
		sb.append("**Entity delete count:** ").append(stats.getEntityDeleteCount()).append('\n');
		sb.append("**Collection fetch count:** ").append(stats.getCollectionFetchCount()).append('\n');
		sb.append("**Collection load count:** ").append(stats.getCollectionLoadCount()).append('\n');
		sb.append("**Cache hit count:** ").append(stats.getSecondLevelCacheHitCount()).append('\n');
		sb.append("**Cache miss count:** ").append(stats.getSecondLevelCacheMissCount()).append('\n');
		sb.append("**Cache put count:** ").append(stats.getSecondLevelCachePutCount()).append('\n');
		return ctx.reply(sb.toString()).then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("dbstats");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Collections.emptySet();
	}

	@Override
	public String getDescription() {
		return "View statistics on database queries made by the bot during its runtime";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.BOT_OWNER;
	}

	@Override
	public EnumSet<Type> getChannelTypesAllowed() {
		return EnumSet.of(Type.GUILD_TEXT, Type.DM);
	}

	@Override
	public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
		return Collections.emptyMap();
	}

}
