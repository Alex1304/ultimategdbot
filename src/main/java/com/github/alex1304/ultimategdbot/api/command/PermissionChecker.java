package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Class used to check permissions for using a command.
 */
public final class PermissionChecker {
	
	private final ConcurrentHashMap<String, Permission> permissionsByName = new ConcurrentHashMap<>();
	private final Set<Permission> unnamedPermissions = synchronizedSet(new HashSet<>());
	
	/**
	 * Registers a new permission into this checker.
	 * 
	 * @param name    a name that uniquely identifies this permission
	 * @param checker a function that takes a Context as input and asynchronously
	 *                tells whether the permission is granted or not. An empty Mono
	 *                is considered the same as false. This function is called when
	 *                using {@link #isGranted(String, Context)}
	 * @throws IllegalArgumentException if a permission with this name already
	 *                                  exists, or if the permission name is blank
	 *                                  as per {@link String#isBlank()}
	 */
	public void register(String name, Function<Context, Mono<Boolean>> checker) {
		requireNonNull(name, "name cannot be null");
		requireNonNull(checker, "checker cannot be null");
		if (name != null && name.isBlank()) {
			throw new IllegalArgumentException("Permission name cannot be blank");
		}
		var perm = new Permission(null, checker);
		if (permissionsByName.putIfAbsent(name, perm) != null) {
			throw new IllegalArgumentException("Attempted to register permission " + name + " more than once");
		}
	}
	
	/**
	 * Registers a new permission into this checker.
	 * 
	 * @param level   the level the permission should be attached to
	 * @param checker a function that takes a Context as input and asynchronously
	 *                tells whether the permission is granted or not. An empty Mono
	 *                is considered the same as false. This function is called when
	 *                using {@link #isGranted(PermissionLevel, Context)}
	 */
	public void register(PermissionLevel level, Function<Context, Mono<Boolean>> checker) {
		requireNonNull(level, "level cannot be null");
		requireNonNull(checker, "checker cannot be null");
		unnamedPermissions.add(new Permission(level, checker));
	}
	
	/**
	 * Copies all permissions registered in the specified permission checker into
	 * this one.
	 * 
	 * @param source the source permission checker
	 */
	@Deprecated
	public void registerAll(PermissionChecker source) {
		requireNonNull(source);
		permissionsByName.putAll(source.permissionsByName);
		unnamedPermissions.addAll(source.unnamedPermissions);
	}
	
	/**
	 * Checks whether the given permission is granted in the specified context. If
	 * the permission has not been registered into this checker, false will be
	 * emitted. If the given permission name is the empty string "", it will always
	 * emit true.
	 * 
	 * @param name the name of the permission to check
	 * @param ctx  the context that applies for the check
	 * @return a Mono that emits true if either the given permission is granted,
	 *         false otherwise.
	 */
	public Mono<Boolean> isGranted(String name, Context ctx) {
		requireNonNull(name, "name cannot be null");
		requireNonNull(ctx, "ctx cannot be null");
		if (name.isEmpty()) {
			return Mono.just(true);
		}
		var perm = permissionsByName.get(name);
		if (perm == null) {
			return Mono.just(false);
		}
		return perm.checker.apply(ctx);
	}
	
	/**
	 * Checks whether the given permission level is granted in the specified
	 * context. If no permission attached to this level is granted, it will check
	 * permissions attached to higher levels. If at least one permission is granted,
	 * true is emitted.
	 * 
	 * @param level the permission level to check
	 * @param ctx   the context that applies for the check
	 * @return a Mono that emits true if any permission attached to the specified
	 *         level or a higher level is granted, false otherwise.
	 * @throws IllegalArgumentException if level is negative
	 */
	public Mono<Boolean> isGranted(PermissionLevel level, Context ctx) {
		requireNonNull(level, "level cannot be null");
		requireNonNull(ctx, "ctx cannot be null");
		if (level == PermissionLevel.PUBLIC) {
			return Mono.just(true);
		}
		return Flux.fromIterable(unnamedPermissions)
				.filter(perm -> perm.level.compareTo(level) <= 0)
				.filterWhen(perm -> perm.checker.apply(ctx))
				.hasElements();
	}
	
	private static class Permission {
		private final PermissionLevel level;
		private final Function<Context, Mono<Boolean>> checker;
		
		Permission(PermissionLevel level, Function<Context, Mono<Boolean>> checker) {
			this.level = level;
			this.checker = checker;
		}
	}
}
