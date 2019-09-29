package com.github.alex1304.ultimategdbot.core;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;
import com.github.alex1304.ultimategdbot.api.utils.menu.InteractiveMenu;
import com.github.alex1304.ultimategdbot.api.utils.menu.UnexpectedReplyException;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="testmenu")
class TestMenuCommand {

	@CommandAction
	public Mono<Void> run(Context ctx) {
		var reactionCount = new AtomicInteger();
		return InteractiveMenu.create("Reply with one of the following: yes or no.\nYou reacted 0 times.")
				.deleteMenuOnClose(ctx.getFlags().has("delete-on-close"))
				.deleteMenuOnTimeout(ctx.getFlags().has("delete-on-timeout"))
				.closeAfterMessage(ctx.getFlags().has("close-after-reply"))
				.closeAfterReaction(ctx.getFlags().has("close-after-reaction"))
				.addMessageItem("yes", replyCtx -> ctx.reply("You said yes!").then())
				.addMessageItem("no", replyCtx -> ctx.reply("You said no!").then())
				.addMessageItem("error", replyCtx -> Mono.error(replyCtx.getFlags().has("fatal") ? new CommandFailedException("Oops!")
						: new UnexpectedReplyException("Oops!")))
				.addReactionItem("success", reactionCtx -> reactionCtx.getMenuMessage().edit(ecs -> ecs.setContent("Reply with one "
						+ "of the following: yes or no.\nYou reacted " + reactionCount.incrementAndGet() + " times.")).then())
				.addReactionItem("😂", reactionCtx -> ctx.reply("lol").then())
				.open(ctx);
	}
}
