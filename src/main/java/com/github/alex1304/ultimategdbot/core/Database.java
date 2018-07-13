package com.github.alex1304.ultimategdbot.core;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.github.alex1304.ultimategdbot.dbentities.AwardedLevel;
import com.github.alex1304.ultimategdbot.dbentities.GDMod;
import com.github.alex1304.ultimategdbot.dbentities.GlobalSettings;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.dbentities.TimelyLevel;
import com.github.alex1304.ultimategdbot.dbentities.UserSettings;

/**
 * Methods to manage database interactions
 *
 * @author Alex1304
 */
public class Database {

	private static SessionFactory sessionFactory = null;
	
	/**
	 * Initializes the database and builds the session factory
	 */
	public static void init() {
		Configuration config = new Configuration();
		config.addClass(GuildSettings.class);
		config.addClass(GlobalSettings.class);
		config.addClass(UserSettings.class);
		config.addClass(AwardedLevel.class);
		config.addClass(TimelyLevel.class);
		config.addClass(GDMod.class);
		sessionFactory = config.buildSessionFactory();
	}
	
	/**
	 * Starts a new database session
	 * 
	 * @return Session
	 */
	public static Session newSession() {
		if (sessionFactory == null)
			throw new IllegalStateException("Database not initialized");
		
		return sessionFactory.openSession();
	}

}
