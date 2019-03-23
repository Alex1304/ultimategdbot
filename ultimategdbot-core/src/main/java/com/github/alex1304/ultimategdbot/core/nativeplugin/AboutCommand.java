package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class AboutCommand implements Command {
	
	private final String aboutText;
	
	public AboutCommand(String aboutText) {
		this.aboutText = Objects.requireNonNull(aboutText);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		return ctx.getBot().getDiscordClients()
				.flatMap(client -> client.getApplicationInfo())
				.next()
				.zipWhen(ApplicationInfo::getOwner)
				.zipWith(Mono.zip(ctx.getBot().getDiscordClients().flatMap(client -> client.getGuilds().count()).collect(Collectors.summingLong(x -> x)),
						ctx.getBot().getDiscordClients().flatMap(client -> client.getUsers().count()).collect(Collectors.summingLong(x -> x))))
				.flatMap(tuple -> {
					var vars = new HashMap<String, String>();
					
					vars.put("bot_name", tuple.getT1().getT1().getName());
					vars.put("project_version", ctx.getBot().getReleaseVersion());
					vars.put("bot_owner", BotUtils.formatDiscordUsername(tuple.getT1().getT2()));
					vars.put("server_count", "" + tuple.getT2().getT1());
					vars.put("user_count", "" + tuple.getT2().getT2());
					vars.put("bot_auth_link", ctx.getBot().getAuthLink());
					vars.put("support_server_invite_link", ctx.getBot().getSupportServerInviteLink());
					String[] result = new String[] { aboutText };
					vars.forEach((k, v) -> result[0] = result[0].replaceAll("\\{\\{ *" + k + " *\\}\\}", String.valueOf(v)));
					return ctx.reply(result[0]);
				}).then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("about");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Collections.emptySet();
	}

	@Override
	public String getDescription() {
		return "Get information about the bot itself.";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.PUBLIC;
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
