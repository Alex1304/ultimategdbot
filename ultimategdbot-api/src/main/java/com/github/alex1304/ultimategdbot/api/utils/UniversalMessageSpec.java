package com.github.alex1304.ultimategdbot.api.utils;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

/**
 * Message specification that is compatible with both {@link MessageCreateSpec}
 * and {@link MessageEditSpec}.
 */
public class UniversalMessageSpec {
	
	private final String content;
	private final Consumer<EmbedCreateSpec> embed;
	
	public UniversalMessageSpec(String content, Consumer<EmbedCreateSpec> embed) {
		this.content = requireNonNull(content);
		this.embed = requireNonNull(embed);
	}
	
	public UniversalMessageSpec(String content) {
		this.content = requireNonNull(content);
		this.embed = null;
	}
	
	public UniversalMessageSpec(Consumer<EmbedCreateSpec> embed) {
		this.content = null;
		this.embed = requireNonNull(embed);
	}
	
	public Consumer<MessageCreateSpec> toMessageCreateSpec() {
		return spec -> {
			if (content != null) {
				spec.setContent(content);
			}
			if (embed != null) {
				spec.setEmbed(embed);
			}
		};
	}
	
	public Consumer<MessageEditSpec> toMessageEditSpec() {
		return spec -> {
			if (content != null) {
				spec.setContent(content);
			}
			if (embed != null) {
				spec.setEmbed(embed);
			}
		};
	}

	public String getContent() {
		return content;
	}

	public Consumer<EmbedCreateSpec> getEmbed() {
		return embed;
	}
}
