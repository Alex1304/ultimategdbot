package com.github.alex1304.ultimategdbot.core;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandSpec;
import com.github.alex1304.ultimategdbot.api.utils.InteractiveMenu;
import com.github.alex1304.ultimategdbot.api.utils.UnexpectedReplyException;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="testmenu")
class TestMenuCommand {

	@CommandAction
	public Mono<Void> run(Context ctx) {
		var reactionCount = new AtomicInteger();
		return InteractiveMenu.create("Reply with one of the following: yes or no.\nYou reacted 0 times.")
				.deleteMenuOnClose(ctx.getFlags().has("delete-on-close"))
				.deleteMenuOnTimeout(ctx.getFlags().has("delete-on-timeout"))
				.closeAfterReply(ctx.getFlags().has("close-after-reply"))
				.closeAfterReaction(ctx.getFlags().has("close-after-reaction"))
				.addReplyItem("yes", replyCtx -> ctx.reply("You said yes!").then())
				.addReplyItem("no", replyCtx -> ctx.reply("You said no!").then())
				.addReplyItem("error", replyCtx -> Mono.error(replyCtx.getFlags().has("fatal") ? new CommandFailedException("Oops!")
						: new UnexpectedReplyException("Oops!")))
				.addReactionItem("success", reactionCtx -> reactionCtx.getMenuMessage().edit(ecs -> ecs.setContent("Reply with one "
						+ "of the following: yes or no.\nYou reacted " + reactionCount.incrementAndGet() + " times.")).then())
				.addReactionItem("😂", reactionCtx -> ctx.reply("lol").then())
				.open(ctx);
	}
}
