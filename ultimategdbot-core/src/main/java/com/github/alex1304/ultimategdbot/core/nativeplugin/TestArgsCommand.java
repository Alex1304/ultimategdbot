package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.Utils;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class TestArgsCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		return ctx.reply(Utils.parseArgs(ctx.getEvent().getMessage().getContent().orElse("No content")).toString()).then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("testargs");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Collections.emptySet();
	}

	@Override
	public String getDescription() {
		return "";
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
