package com.github.alex1304.ultimategdbot.core;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandPermission;
import com.github.alex1304.ultimategdbot.api.util.menu.InteractiveMenu;
import com.github.alex1304.ultimategdbot.core.database.CoreConfigDao;

import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFieldData;
import discord4j.discordjson.json.ImmutableEmbedAuthorData;
import discord4j.discordjson.json.ImmutableEmbedData;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.json.ImmutableMessageCreateRequest;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@CommandDescriptor(
		aliases = "changelog",
		shortDescription = "Sends a changelog to all guilds that are configured to receive them."
)
@CommandPermission(level = PermissionLevel.BOT_OWNER)
public class ChangelogCommand {

	private final HttpClient fileClient = HttpClient.create().headers(h -> h.add("Content-Type", "text/plain"));
	
	@CommandAction
	@CommandDoc("This command expects one text file attached to the message. This textfile contains "
			+ "information that should be included in the announcement, in the following format:\n"
			+ "```\n"
			+ "First line is the title of the announcement\n\n"
			+ "Skip two lines, and write the title of the first section\n"
			+ "On next line, the content of the first section\n\n"
			+ "Skip two lines again and write the title of the 2nd section\n"
			+ "Then on next line the content of the 2nd section, etc etc.\n"
			+ "```\n")
	public Mono<Void> run(Context ctx) {
		if (ctx.event().getMessage().getAttachments().size() != 1) {
			return Mono.error(new CommandFailedException("You must attach exactly one file."));
		}
		return getFileContent(ctx.event().getMessage().getAttachments().stream().findAny().orElseThrow())
				.map(String::lines)
				.flatMapMany(Flux::fromStream)
				.filter(l -> !l.startsWith("#"))
				.collectList()
				.map(lines -> parse(ctx.author(), lines))
				.flatMap(embedData -> InteractiveMenu.create(m -> {
							m.setContent("Here is the announcement that is going to be sent to all servers. Is this alright? React to confirm.");
							m.setEmbed(embed -> {
								embed.setTitle(embedData.title().get());
								embed.setColor(embedData.color().get());
								embedData.fields().get().forEach(fieldData ->
										embed.addField(fieldData.name(), fieldData.value(), fieldData.inline().get()));
							});
						})
						.addReactionItem("success", interaction -> ctx.reply("Sending announcement, please wait...")
								.then(ctx.bot().database().withExtension(CoreConfigDao.class, CoreConfigDao::getAllChangelogChannels)
										.flatMapMany(Flux::fromIterable)
										.map(ctx.bot().rest()::getChannelById)
										.flatMap(channel -> channel.createMessage(ImmutableMessageCreateRequest.builder()
												.embed(Possible.of(embedData))
												.build())
												.onErrorResume(e -> Mono.empty()))
										.then(ctx.reply("Announcement sent to all guilds!")))
								.then())
						.addReactionItem("cross", interaction -> Mono.fromRunnable(interaction::closeMenu))
						.deleteMenuOnClose(true)
						.open(ctx))
				.then();
	}
	
	private EmbedData parse(User author, List<String> lines) {
		final var expectingFieldName = 1;
		final var expectingFieldContent = 2;
		var state = 0;
		String title = null;
		var fieldNames = new ArrayList<String>();
		var fieldContents = new ArrayList<String>();
		for (var l : lines) {
			if (title == null) {
				title = l;
				state = expectingFieldName;
			} else {
				if (state == expectingFieldName) {
					if (!l.isBlank()) {
						fieldNames.add(l);
						state = expectingFieldContent;
					}
				} else {
					if (fieldContents.size() < fieldNames.size()) {
						if (!l.isBlank()) {
							fieldContents.add(l);
						}
					} else {
						if (l.isBlank()) {
							state = expectingFieldName;
						} else {
							var lastIndex = fieldContents.size() - 1;
							var newL = fieldContents.get(lastIndex) + "\n" + l;
							fieldContents.set(lastIndex, newL);
						}
					}
				}
			}
		}
		if (title == null || fieldNames.size() != fieldContents.size()) {
			throw new CommandFailedException("The input file has invalid or malformed content.");
		}
		var fTitle = title;
		var fields = new ArrayList<EmbedFieldData>();
		for (var i = 0 ; i < fieldNames.size() ; i++) {
			var name = fieldNames.get(i);
			var content = fieldContents.get(i);
			fields.add(ImmutableEmbedFieldData.builder()
					.name(name)
					.value(content)
					.inline(Possible.of(false))
					.build());
		}
		return ImmutableEmbedData.builder()
				.title(Possible.of(fTitle))
				.color(Possible.of(0x0000FF))
				.timestamp(Possible.of(DateTimeFormatter.ISO_INSTANT.format(Instant.now())))
				.author(Possible.of(ImmutableEmbedAuthorData.builder()
						.name(Possible.of(author.getTag()))
						.iconUrl(Possible.of(author.getAvatarUrl()))
						.build()))
				.fields(Possible.of(fields))
				.build();
	}
	
	private Mono<String> getFileContent(Attachment attachment) {
		return fileClient.get()
				.uri(attachment.getUrl())
				.responseSingle((response, content) -> {
					if (response.status().code() / 100 != 2) {
						return Mono.error(new CommandFailedException("Received " + response.status().code() + " "
								+ response.status().reasonPhrase() + " from Discord CDN"));
					}
					return content.asString();
				})
				.retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
						.maxBackoff(Duration.ofMinutes(1))
						.filter(IOException.class::isInstance))
				.timeout(Duration.ofMinutes(2), Mono.error(new CommandFailedException("Cannot download file, Discord "
						+ "CDN took too long to respond. Try again later.")));
	}
}
