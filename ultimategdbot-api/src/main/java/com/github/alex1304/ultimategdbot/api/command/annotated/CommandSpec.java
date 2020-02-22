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
	String shortDescription() default "";
	PermissionLevel minimumPermissionLevel() default PermissionLevel.PUBLIC;
	String requiredPermission() default "";
	Scope scope() default Scope.ANYWHERE;
}
