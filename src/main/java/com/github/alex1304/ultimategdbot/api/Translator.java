package com.github.alex1304.ultimategdbot.api;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A class implementing this interface is able to translate strings.
 */
public interface Translator {
	
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
