package com.github.alex1304.ultimategdbot.api.command.annotated;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;

@Documented
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface CommandPermission {
	
	String name() default "";
	PermissionLevel level() default PermissionLevel.PUBLIC;
}
