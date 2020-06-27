package com.github.alex1304.ultimategdbot.api.service;

/**
 * Represents a service provided by the bot, accessible globally via
 * {@link ServiceContainer}. A service is created on bot startup and lives until
 * the bot disconnects.
 */
public interface Service extends ServiceDependant {
}
