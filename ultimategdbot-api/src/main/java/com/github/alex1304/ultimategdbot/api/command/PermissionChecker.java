package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * Class used to check permissions for using a command.
 */
public final class PermissionChecker {
	
	private final ConcurrentHashMap<String, Permission> permissionsByName = new ConcurrentHashMap<>();
	private final Set<Permission> unnamedPermissions = synchronizedSet(new HashSet<>());
	
	/**
	 * Registers a new permission into this checker.
	 * 
	 * @param level   the level the permission should be attached to
	 * @param name    a name that uniquely identifies this permission. May be null
	 * @param checker a function that takes a Context as input and asynchronously
	 *                tells whether the permission is granted or not. An empty Mono
	 *                is considered the same as false. This function is called when
	 *                using {@link #isGranted(String, Context)} or
	 *                {@link #isGranted(PermissionLevel, Context)}
	 * @throws IllegalArgumentException if a permission with this name already
	 *                                  exists, or if the permission name is blank
	 *                                  as per {@link String#isBlank()}
	 */
	public void register(PermissionLevel level, @Nullable String name, Function<Context, Mono<Boolean>> checker) {
		requireNonNull(level, "level cannot be null");
		requireNonNull(checker, "checker cannot be null");
		if (name != null && name.isBlank()) {
			throw new IllegalArgumentException("Permission name cannot be blank");
		}
		var perm = new Permission(level, checker);
		if (name != null) {
			if (permissionsByName.putIfAbsent(name, perm) != null) {
				throw new IllegalArgumentException("Attempted to register permission " + name + " more than once");
			}
		} else {
			unnamedPermissions.add(perm);
		}
	}
	
	/**
	 * Registers a new permission into this checker. This is equivalent to
	 * {@link #register(PermissionLevel, String, Function)} with null as the second
	 * argument.
	 * 
	 * @param level   the level the permission should be attached to
	 * @param checker a function that takes a Context as input and asynchronously
	 *                tells whether the permission is granted or not. An empty Mono
	 *                is considered the same as false. This function is called when
	 *                using {@link #isGranted(String, Context)} or
	 *                {@link #isGranted(PermissionLevel, Context)}
	 */
	public void register(PermissionLevel level, Function<Context, Mono<Boolean>> checker) {
		register(level, null, checker);
	}
	
	/**
	 * Copies all permissions registered in the specified permission checker into
	 * this one.
	 * 
	 * @param source the source permission checker
	 */
	public void registerAll(PermissionChecker source) {
		requireNonNull(source);
		permissionsByName.putAll(source.permissionsByName);
		unnamedPermissions.addAll(source.unnamedPermissions);
	}
	
	/**
	 * Checks whether the given permission is granted in the specified context. If
	 * the permission has not been registered into this checker, false will be
	 * emitted. If the permission isn't granted, it will check if at least one
	 * permission that is higher level is granted, but not the other ones on the
	 * same level. If neither the given permission nor permissions that are higher
	 * level than the given one are granted, false is emitted. Otherwise emits true.
	 * 
	 * @param name the name of the permission to check
	 * @param ctx  the context that applies for the check
	 * @return a Mono that emits true if either the given permission or a permission
	 *         that is higher level is granted, false otherwise.
	 */
	public Mono<Boolean> isGranted(String name, Context ctx) {
		requireNonNull(name, "name cannot be null");
		requireNonNull(ctx, "ctx cannot be null");
		var perm = permissionsByName.get(name);
		if (perm == null) {
			return Mono.just(false);
		}
		return perm.checker.apply(ctx)
				.filter(isGranted -> isGranted)
				.switchIfEmpty(Mono.defer(() -> isGranted0(perm.level, ctx, true)));
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
		return isGranted0(level, ctx, false);
	}
	
	private Mono<Boolean> isGranted0(PermissionLevel level, Context ctx, boolean onlyHigherLevels) {
		requireNonNull(level, "level cannot be null");
		requireNonNull(ctx, "ctx cannot be null");
		return allPermissions()
				.filter(perm -> onlyHigherLevels ? perm.level.compareTo(level) < 0 : perm.level.compareTo(level) <= 0)
				.filterWhen(perm -> perm.checker.apply(ctx))
				.hasElements();
	}
	
	private Flux<Permission> allPermissions() {
		return Flux.merge(
				Flux.fromIterable(permissionsByName.values()),
				Flux.fromIterable(unnamedPermissions));
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
