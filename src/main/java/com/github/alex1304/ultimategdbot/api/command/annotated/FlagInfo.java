package com.github.alex1304.ultimategdbot.api.command.annotated;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface FlagInfo {
	String name();
	String valueFormat() default "";
	String description();
}
