package com.github.alex1304.ultimategdbot.api.utils.reply;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public class PaginatedReplyMenuBuilder extends ReplyMenuBuilder {
	private final Command cmd;

	public PaginatedReplyMenuBuilder(Command cmd, Context ctx, boolean deleteOnReply, boolean deleteOnTimeout) {
		super(ctx, deleteOnReply, deleteOnTimeout);
		this.cmd = Objects.requireNonNull(cmd);
	}
	
	@Override
	public void setHeader(String header) {
		throw new UnsupportedOperationException("Headers for paginated reply menus are not customizable");
	}
	
	@Override
	public Mono<Message> build(String content, Consumer<EmbedCreateSpec> embed) {
		@SuppressWarnings("unchecked")
		var pages = (List<String>) ctx.getVar("pages", List.class);
		if (pages == null) {
			pages = BotUtils.chunkMessage(content);
			ctx.setVar("pages", pages);
			ctx.setVar("page", 0);
			ctx.setVar("pageMax", pages.size() - 1);
		}
		var page = ctx.getVar("page", Integer.class);
		var pageMax = ctx.getVar("pageMax", Integer.class);
		super.setHeader(String.format("Page %d/%d", page + 1, pageMax + 1));
		if (page < pageMax) {
			addItem("next", "To go to next page, type `next`", ctx0 -> {
				ctx.setVar("page", ctx.getVarOrDefault("page", 0) + 1);
				Command.invoke(cmd, ctx);
				return Mono.empty();
			});
		}
		if (page > 0) {
			addItem("prev", "To go to previous page, type `prev`", ctx0 -> {
				ctx.setVar("page", ctx.getVarOrDefault("page", 0) - 1);
				Command.invoke(cmd, ctx);
				return Mono.empty();
			});
		}
		if (pageMax > 0) {
			addItem("page", "To go to a specific page, type `page` followed by the page number, ex `page 3`", ctx0 -> {
				if (ctx0.getArgs().size() == 1) {
					Command.invoke(cmd, ctx);
					return Mono.error(new CommandFailedException("Please specify a page number"));
				}
				try {
					var page0 = Integer.parseUnsignedInt(ctx0.getArgs().get(1)) - 1;
					var pageMax0 = ctx.getVar("pageMax", Integer.class);
					if (page0 > pageMax0) {
						Command.invoke(cmd, ctx);
						return Mono.error(new CommandFailedException("Page number out of range"));
					}
					ctx.setVar("page", page0);
					Command.invoke(cmd, ctx);
					return Mono.empty();
				} catch (NumberFormatException e) {
					Command.invoke(cmd, ctx);
					return Mono.error(new CommandFailedException("Please specify a valid page number"));
				}
			});
		}
		return super.build(pages.isEmpty() ? "" : pages.get(page), embed);
	}
}
