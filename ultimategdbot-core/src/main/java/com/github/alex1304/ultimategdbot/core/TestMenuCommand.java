package com.github.alex1304.ultimategdbot.core;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandSpec;
import com.github.alex1304.ultimategdbot.api.utils.InteractiveMenu;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="testmenu")
class TestMenuCommand {

	@CommandAction
	public Mono<Void> run(Context ctx) {
		var reactionCount = new AtomicInteger();
		return InteractiveMenu.create("Reply with one of the following: yes or no.\n"
				+ "You reacted 0 times.")
				.deleteMenuOnReply(ctx.getFlags().has("close"))
				.addReplyItem("yes", (menu, ctx0) -> ctx0.reply("You said yes!").then())
				.addReplyItem("no", (menu, ctx0) -> ctx0.reply("You said no!").then())
				.addReactionItem("success", (menu, reactionAddEvent) -> menu.edit(ecs -> ecs.setContent("Reply with one of the following: yes or no.\n"
				+ "You reacted " + reactionCount.incrementAndGet() + " times.")).then())
				.open(ctx);
	}
}
