package com.github.alex1304.ultimategdbot.core.database;

import static java.util.Objects.requireNonNullElse;

public class BlacklistedIds {
	
	private long id;

	public long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = requireNonNullElse(id, 0L);
	}
}
