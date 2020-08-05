package com.github.alex1304.ultimategdbot.api.localization;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.github.alex1304.ultimategdbot.api.BotConfig;
import com.github.alex1304.ultimategdbot.api.Translator;

import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

public final class LocalizationService implements Translator {
	
	public static final String CONFIG_RESOURCE_NAME = "localization";
	
	private static final Logger LOGGER = Loggers.getLogger(LocalizationService.class);
	
	private final Set<Locale> supportedLocales;
	private final Locale defaultLocale;
	private final ConcurrentHashMap<Long, Locale> localeByGuild = new ConcurrentHashMap<>();

	public LocalizationService(BotConfig botConfig) {
		this.defaultLocale = botConfig.resource(CONFIG_RESOURCE_NAME)
				.readOptional("default_locale")
				.map(Locale::forLanguageTag)
				.orElse(Locale.getDefault());
		this.supportedLocales = Stream
				.concat(botConfig.resource(CONFIG_RESOURCE_NAME)
								.readAsStream("supported_locales", ",")
								.map(Locale::forLanguageTag),
						Stream.of(defaultLocale))
				.collect(toUnmodifiableSet());
	}

	/**
	 * Finds the locale specific to the given guild.
	 * 
	 * @param guildId the guild id
	 * @return the locale used by the guild
	 */
	public Locale getLocaleForGuild(long guildId) {
		return localeByGuild.getOrDefault(guildId, defaultLocale);
	}

	/**
	 * Sets a locale specific for the given guild. If one was already set for the
	 * same guild, it is overwritten.
	 * 
	 * @param guildId the guild id
	 * @param locale  the new locale. May be null, in which case the locale is
	 *                removed.
	 */
	public void setLocaleForGuild(long guildId, @Nullable Locale locale) {
		if (locale == null) {
			localeByGuild.remove(guildId);
			LOGGER.debug("Removed locale for guild {}", guildId);
			return;
		}
		localeByGuild.put(guildId, locale);
		LOGGER.debug("Changed locale for guild {}: {}", guildId, locale);
	}

	/**
	 * Gets the set of locales supported by this service.
	 * 
	 * @return the locales supported
	 */
	public Set<Locale> getSupportedLocales() {
		return supportedLocales;
	}

	@Override
	public Locale getLocale() {
		return defaultLocale;
	}
}
