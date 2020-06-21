package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.Translator;
import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;

public class ParamConversionException extends CommandFailedException {
	private static final long serialVersionUID = -6218361940906651280L;

	public ParamConversionException(Translator tr, String param, String arg, String message) {
		super(message == null
				? tr.translate("CommonStrings", "conversion_error", arg, param)
				: tr.translate("CommonStrings", "conversion_error_with_reason", arg, param, message));
	}
}
