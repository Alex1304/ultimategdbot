package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import java.util.function.Function;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Maps a guild setting class with its instance function
 *
 * @author Alex1304
 */
public class GuildSettingMapping<I, O, T extends GuildSetting<I, O>> {
	
	private String name;
	private Class<T> settingClass;
	private Function<GuildSettings, T> instanceFunc;
	
	public GuildSettingMapping(String name, Class<T> settingClass, Function<GuildSettings, T> instanceFunc) {
		this.name = name;
		this.settingClass = settingClass;
		this.instanceFunc = instanceFunc;
	}

	/**
	 * Gets the name
	 *
	 * @return String
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the settingClass
	 *
	 * @return Class<T>
	 */
	public Class<T> getSettingClass() {
		return settingClass;
	}
	
	/**
	 * Gets the instanceFunc
	 *
	 * @return Function<GuildSettings,T>
	 */
	public Function<GuildSettings, T> getInstanceFunc() {
		return instanceFunc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GuildSettingMapping))
			return false;
		GuildSettingMapping<?, ?, ?> other = (GuildSettingMapping<?, ?, ?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}