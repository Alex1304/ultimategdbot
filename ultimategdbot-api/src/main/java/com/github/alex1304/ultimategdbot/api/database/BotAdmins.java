package com.github.alex1304.ultimategdbot.api.database;

import static java.util.Objects.requireNonNullElse;

public class BotAdmins {
	
	private long userId;

	public long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = requireNonNullElse(userId, 0L);
	}
}
