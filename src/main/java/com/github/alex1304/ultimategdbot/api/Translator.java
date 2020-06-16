package com.github.alex1304.ultimategdbot.api;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A class implementing this interface is able to translate strings.
 */
@FunctionalInterface
public interface Translator {
	
	/**
	 * Creates a new {@link Translator} that translates to the target locale.
	 * 
	 * @param locale the target locale
	 * @return a new {@link Translator}
	 */
	static Translator to(Locale locale) {
		return () -> locale;
	}
	
	/**
	 * Translates a string.
	 * 
	 * @param bundle the name of the bundle where to find the strings
	 * @param key the key identifying the string
	 * @param args the arguments of the string, if any
	 * @return the translated string
	 */
	default String translate(String bundle, String key, Object... args) {
		return String.format(ResourceBundle.getBundle(bundle, getLocale()).getString(key), args);
	}
	
	/**
	 * Gets the locale used by this translator.
	 * 
	 * @return the locale
	 */
	Locale getLocale();
}
