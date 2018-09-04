package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;

/**
 * Represents a guild setting
 * 
 * @param <I>
 *            - The input type of the setting
 * @param <O>
 *            - The output type of the settng
 *
 * @author Alex1304
 */
public abstract class GuildSetting<I, O> {
	
	private static final List<GuildSettingMapping<?, ?, ?>> REGISTERED_SETTINGS = registerSettings();
	
	protected GuildSettings gs;
	protected Function<GuildSettings, I> valueGetter;
	protected BiConsumer<GuildSettings, I> valueSetter;
	protected O value;
	
	public GuildSetting(GuildSettings gs, Function<GuildSettings, I> valueGetter, BiConsumer<GuildSettings, I> valueSetter) {
		this.gs = gs;
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
		this.value = null;
	}
	
	private static List<GuildSettingMapping<?, ?, ?>> registerSettings() {
		List<GuildSettingMapping<?, ?, ?>> theList = new ArrayList<>();
		
		theList.add(new GuildSettingMapping<>("channel_awarded_levels", ChannelAwardedLevelsSetting.class, x -> new ChannelAwardedLevelsSetting(x)));
		theList.add(new GuildSettingMapping<>("channel_timely_levels", ChannelTimelyLevelsSetting.class, x -> new ChannelTimelyLevelsSetting(x)));
		theList.add(new GuildSettingMapping<>("channel_gd_moderators", ChannelGDModeratorsSetting.class, x -> new ChannelGDModeratorsSetting(x)));
		theList.add(new GuildSettingMapping<>("role_awarded_levels", RoleAwardedLevelsSetting.class, x -> new RoleAwardedLevelsSetting(x)));
		theList.add(new GuildSettingMapping<>("role_timely_levels", RoleTimelyLevelsSetting.class, x -> new RoleTimelyLevelsSetting(x)));
		theList.add(new GuildSettingMapping<>("role_gd_moderators", RoleGDModeratorsSetting.class, x -> new RoleGDModeratorsSetting(x)));
		theList.add(new GuildSettingMapping<>("channel_changelog", ChannelChangelogSetting.class, x -> new ChannelChangelogSetting(x)));
		
		return theList;
	}
	
	public static GuildSetting<?, ?> get(String name, GuildSettings gs) {
		GuildSettingMapping<?, ?, ?> gsm = null;
		Iterator<GuildSettingMapping<?, ?, ?>> it = REGISTERED_SETTINGS.iterator();
		
		while (gsm == null && it.hasNext()) {
			gsm = it.next();
			if (!gsm.getName().equals(name))
				gsm = null;
		}
		
		if (gsm == null)
			return null;
		
		return gsm.getInstanceFunc().apply(gs);
	}
	
	public static Iterator<GuildSettingMapping<?, ?, ?>> iterateSettings() {
		return REGISTERED_SETTINGS.iterator();
	}
	
	/**
	 * Builds the output value by processing the input value
	 * If the input value is invalid, it is intended to return null
	 * 
	 * @return
	 */
	protected abstract O buildValue();
	
	/**
	 * Parses the string into a value ready to put in database. It is intended
	 * to throw a <code>IllegalArgumentException</code> if str doesn't match
	 * with a valid value.
	 * 
	 * @param str
	 *            - String
	 * @return O
	 */
	protected abstract O parseValue(String str);
	
	/**
	 * Converts the value to its database type
	 * 
	 * @param value - O
	 * @return I
	 */
	public abstract I valueToDatabaseType(O value);
	
	@Override
	public String toString() {
		return String.valueOf(getValue());
	}
	
	/**
	 * Gets the value
	 * 
	 * @return O
	 */
	public O getValue() {
		if (value != null)
			return value;
		else
			return buildValue();
	}
	
	/**
	 * Saves the given value in database
	 * 
	 * @param value - O
	 */
	public void save(O value) {
		valueSetter.accept(gs, valueToDatabaseType(value));
		DatabaseUtils.save(gs);
	}
	
	/**
	 * Parses the value then saves it to database
	 * 
	 * @param valueStr
	 *            - String
	 * @throws IllegalArgumentException
	 *             if the string fails to parse
	 */
	public void save(String valueStr) {
		if (valueStr.isEmpty())
			save((O) null);
		else
			save(parseValue(valueStr));
	}
}
