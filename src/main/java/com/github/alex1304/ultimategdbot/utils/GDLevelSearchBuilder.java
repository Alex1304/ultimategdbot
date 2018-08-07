package com.github.alex1304.ultimategdbot.utils;

import java.util.HashSet;
import java.util.Set;

import com.github.alex1304.jdash.api.request.GDLevelSearchHttpRequest;
import com.github.alex1304.jdash.util.Constants;

/**
 * Builds an HTTP request to search for levels with the desired criterias.
 *
 * @author Alex1304
 */
public class GDLevelSearchBuilder {
	
	private int type;
	private String keywords;
	private Set<Integer> difficulties;
	private Set<Integer> lengths;
	private	int page;
	private boolean uncompleted;
	private boolean onlyCompleted;
	private boolean featured;
	private boolean original;
	private boolean twoPlayer;
	private boolean coins;
	private boolean epic;
	private boolean star;
	private int demonFilter;
	
	public GDLevelSearchBuilder() {
		this.type = Constants.LEVEL_SEARCH_TYPE_REGULAR;
		this.difficulties = new HashSet<>();
		this.lengths = new HashSet<>();
	}
	
	public GDLevelSearchHttpRequest build() {
		return new GDLevelSearchHttpRequest(type, keywords, difficulties, lengths, page, uncompleted, onlyCompleted, featured, original, twoPlayer, coins, epic, star, demonFilter);
	}
	
	/**
	 * Gets the type
	 *
	 * @return int
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Gets the keywords
	 *
	 * @return String
	 */
	public String getKeywords() {
		return keywords;
	}
	
	/**
	 * Gets the difficulties
	 *
	 * @return Set<Integer>
	 */
	public Set<Integer> getDifficulties() {
		return difficulties;
	}
	
	/**
	 * Gets the lengths
	 *
	 * @return Set<Integer>
	 */
	public Set<Integer> getLengths() {
		return lengths;
	}
	
	/**
	 * Gets the page
	 *
	 * @return int
	 */
	public int getPage() {
		return page;
	}
	
	/**
	 * Gets the uncompleted
	 *
	 * @return boolean
	 */
	public boolean isUncompleted() {
		return uncompleted;
	}
	
	/**
	 * Gets the onlyCompleted
	 *
	 * @return boolean
	 */
	public boolean isOnlyCompleted() {
		return onlyCompleted;
	}
	
	/**
	 * Gets the featured
	 *
	 * @return boolean
	 */
	public boolean isFeatured() {
		return featured;
	}
	
	/**
	 * Gets the original
	 *
	 * @return boolean
	 */
	public boolean isOriginal() {
		return original;
	}
	
	/**
	 * Gets the twoPlayer
	 *
	 * @return boolean
	 */
	public boolean isTwoPlayer() {
		return twoPlayer;
	}
	
	/**
	 * Gets the coins
	 *
	 * @return boolean
	 */
	public boolean isCoins() {
		return coins;
	}
	
	/**
	 * Gets the epic
	 *
	 * @return boolean
	 */
	public boolean isEpic() {
		return epic;
	}
	
	/**
	 * Gets the star
	 *
	 * @return boolean
	 */
	public boolean isStar() {
		return star;
	}
	
	/**
	 * Gets the demonFilter
	 *
	 * @return int
	 */
	public int getDemonFilter() {
		return demonFilter;
	}
	
	/**
	 * Sets the type
	 *
	 * @param type - int
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * Sets the keywords
	 *
	 * @param keywords - String
	 */
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	/**
	 * Sets the difficulties
	 *
	 * @param difficulties - Set<Integer>
	 */
	public void setDifficulties(Set<Integer> difficulties) {
		this.difficulties = difficulties;
	}
	
	/**
	 * Sets the lengths
	 *
	 * @param lengths - Set<Integer>
	 */
	public void setLengths(Set<Integer> lengths) {
		this.lengths = lengths;
	}
	
	/**
	 * Sets the page
	 *
	 * @param page - int
	 */
	public void setPage(int page) {
		this.page = page;
	}
	
	/**
	 * Sets the uncompleted
	 *
	 * @param uncompleted - boolean
	 */
	public void setUncompleted(boolean uncompleted) {
		this.uncompleted = uncompleted;
	}
	
	/**
	 * Sets the onlyCompleted
	 *
	 * @param onlyCompleted - boolean
	 */
	public void setOnlyCompleted(boolean onlyCompleted) {
		this.onlyCompleted = onlyCompleted;
	}
	
	/**
	 * Sets the featured
	 *
	 * @param featured - boolean
	 */
	public void setFeatured(boolean featured) {
		this.featured = featured;
	}
	
	/**
	 * Sets the original
	 *
	 * @param original - boolean
	 */
	public void setOriginal(boolean original) {
		this.original = original;
	}
	
	/**
	 * Sets the twoPlayer
	 *
	 * @param twoPlayer - boolean
	 */
	public void setTwoPlayer(boolean twoPlayer) {
		this.twoPlayer = twoPlayer;
	}
	
	/**
	 * Sets the coins
	 *
	 * @param coins - boolean
	 */
	public void setCoins(boolean coins) {
		this.coins = coins;
	}
	
	/**
	 * Sets the epic
	 *
	 * @param epic - boolean
	 */
	public void setEpic(boolean epic) {
		this.epic = epic;
	}
	
	/**
	 * Sets the star
	 *
	 * @param star - boolean
	 */
	public void setStar(boolean star) {
		this.star = star;
	}
	
	/**
	 * Sets the demonFilter
	 *
	 * @param demonFilter - int
	 */
	public void setDemonFilter(int demonFilter) {
		this.demonFilter = demonFilter;
	}
}
