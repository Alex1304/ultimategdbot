package com.github.alex1304.ultimategdbot.api.util.menu;

import static java.util.Objects.requireNonNull;

public class PaginationControls {

	private final String previousEmoji;
	private final String nextEmoji;
	private final String closeEmoji;
	
	public PaginationControls(String previousEmoji, String nextEmoji, String closeEmoji) {
		this.previousEmoji = requireNonNull(previousEmoji);
		this.nextEmoji = requireNonNull(nextEmoji);
		this.closeEmoji = requireNonNull(closeEmoji);
	}
	
	public String getPreviousEmoji() {
		return previousEmoji;
	}
	
	public String getNextEmoji() {
		return nextEmoji;
	}
	
	public String getCloseEmoji() {
		return closeEmoji;
	}

	public static PaginationControls getDefault() {
		return new PaginationControls("◀️", "▶️", "🚫");
	}
}
