package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import static com.github.alex1304.ultimategdbot.api.util.Markdown.code;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;

public class ParamConversionException extends CommandFailedException {
	private static final long serialVersionUID = -6218361940906651280L;

	public ParamConversionException(String param, String arg, String message) {
		super("Cannot convert " + code(arg) + " into " + code(param) + (message == null ? "" : ": " + message));
	}
}
