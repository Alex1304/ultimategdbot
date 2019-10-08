package com.github.alex1304.ultimategdbot.api.command.annotated;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.Scope;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface CommandSpec {
	String[] aliases();
	PermissionLevel permLevel() default PermissionLevel.PUBLIC;
	Scope scope() default Scope.ANYWHERE;
	String shortDescription() default "";
}
