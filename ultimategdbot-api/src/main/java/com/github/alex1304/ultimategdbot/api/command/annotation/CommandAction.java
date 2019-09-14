package com.github.alex1304.ultimategdbot.api.command.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.alex1304.ultimategdbot.api.command.parser.Parser;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface CommandAction {
	Class<? extends Parser<?>>[] value() default {};
}
