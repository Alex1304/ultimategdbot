package com.github.alex1304.ultimategdbot.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.List;

public class GatewayEventFilter extends TurboFilter {

	private String logger;
	private final List<String> includedEvents = new ArrayList<>();
	private final List<String> excludedEvents = new ArrayList<>();

	@Override
	public FilterReply decide(Marker marker, Logger log, Level level, String format, Object[] params, Throwable t) {
		String logName = log.getName();
		if (logName.equals(logger)
				|| (logger == null && logName.endsWith("protocol.sender") || logName.endsWith("protocol.receiver"))) {
			if (format != null) {
				if (!excludedEvents.isEmpty() && excludedEvents.stream().anyMatch(format::contains)) {
					return FilterReply.DENY;
				}
				if (!includedEvents.isEmpty() && includedEvents.stream().noneMatch(format::contains)) {
					return FilterReply.DENY;
				}
			}
		}
		return FilterReply.NEUTRAL;
	}

	public void setLogger(String logger) {
		this.logger = logger;
	}

	public void addInclude(String include) {
		this.includedEvents.add(include);
	}

	public void addExclude(String exclude) {
		this.excludedEvents.add(exclude);
	}
}