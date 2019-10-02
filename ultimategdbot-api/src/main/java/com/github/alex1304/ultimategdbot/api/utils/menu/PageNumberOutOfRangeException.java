package com.github.alex1304.ultimategdbot.api.utils.menu;

public class PageNumberOutOfRangeException extends RuntimeException {
	private static final long serialVersionUID = -7545514938872465358L;

	private final int minPage;
	private final int maxPage;

	public PageNumberOutOfRangeException(int minPage, int maxPage) {
		if (minPage > maxPage) {
			throw new IllegalArgumentException("minPage > maxPage");
		}
		this.minPage = minPage;
		this.maxPage = maxPage;
	}

	public int getMinPage() {
		return minPage;
	}

	public int getMaxPage() {
		return maxPage;
	}
	
	public static void check(int value, int min, int max) {
		if (value < min || value > max) {
			throw new PageNumberOutOfRangeException(min, max);
		}
	}
}
