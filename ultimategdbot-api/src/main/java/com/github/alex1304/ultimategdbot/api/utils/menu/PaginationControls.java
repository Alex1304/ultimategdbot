package com.github.alex1304.ultimategdbot.api.utils.menu;

import java.util.Objects;

public class PaginationControls {

	private final String previousEmoji;
	private final String nextEmoji;
	private final String closeEmoji;
	
	public PaginationControls(String previousEmoji, String nextEmoji, String closeEmoji) {
		this.previousEmoji = Objects.requireNonNull(previousEmoji);
		this.nextEmoji = Objects.requireNonNull(nextEmoji);
		this.closeEmoji = Objects.requireNonNull(closeEmoji);
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
}
