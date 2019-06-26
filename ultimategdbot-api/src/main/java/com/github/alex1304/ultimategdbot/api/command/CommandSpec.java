package com.github.alex1304.ultimategdbot.api.command;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.alex1304.ultimategdbot.api.command.Command.Scope;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface CommandSpec {
	String[] aliases();
	PermissionLevel permLevel() default PermissionLevel.PUBLIC;
	Scope scope() default Scope.ANYWHERE;
}
