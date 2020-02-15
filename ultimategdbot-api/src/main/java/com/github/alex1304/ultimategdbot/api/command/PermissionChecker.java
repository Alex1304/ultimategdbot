package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Class used to check permissions for using a command.
 */
public class PermissionChecker {
	
	private final ConcurrentHashMap<String, Permission> permissionsByName = new ConcurrentHashMap<>();
	
	public PermissionChecker() {
		var botOwnerPerm = new Permission(0, ctx -> ctx.getBot().getOwnerId()
				.map(id -> id.longValue() == ctx.getAuthor().getId().asLong()));
		permissionsByName.put(CommonPermissions.BOT_OWNER, botOwnerPerm);
	}
	
	/**
	 * Registers a new permission into this checker. Newly registered permissions
	 * can further be checked using the {@link #isGranted(String, Context)} method.
	 * 
	 * @param permissionName    the name of the permission to registered
	 * @param permissionLevel   the level the permission belongs to
	 * @param permissionChecker a function that takes a Context as input and
	 *                          asynchronously tells whether the permission is
	 *                          granted or not. An empty Mono is considered the same
	 *                          as false. It's this function that's gonna be called
	 *                          when using {@link #isGranted(String, Context)}
	 */
	public void register(String permissionName, int permissionLevel, Function<Context, Mono<Boolean>> permissionChecker) {
		requireNonNull(permissionName);
		requireNonNull(permissionChecker);
		if (permissionLevel < 0) {
			throw new IllegalArgumentException("Permission level must be >= 1, " + permissionLevel + " given");
		}
		if (permissionLevel == 0) {
			throw new IllegalArgumentException("Cannot attach permission to level 0 as it is reserved for bot owner");
		}
		var perm = new Permission(permissionLevel, permissionChecker);
		permissionsByName.put(permissionName, perm);
	}
	
	/**
	 * Checks whether the given permission is granted in the specified context. If
	 * the permission has not been registered into this checker, false will be
	 * emitted. If the permission isn't granted, it will check if at least one
	 * permission that is higher level is granted. If neither the given permission
	 * nor permissions that are higher level than the given one are granted, false
	 * is emitted. Otherwise emits true.
	 * 
	 * @param permissionName the name of the permission to check
	 * @param ctx            the context that applies for the check
	 * @return a Mono that emits true if either the given permission or a permission
	 *         that is higher level is granted, false otherwise.
	 */
	public Mono<Boolean> isGranted(String permissionName, Context ctx) {
		requireNonNull(permissionName);
		requireNonNull(ctx);
		var perm = permissionsByName.get(permissionName);
		if (perm == null) {
			return Mono.just(false);
		}
		return perm.checker.apply(ctx)
				.filter(isGranted -> isGranted)
				.switchIfEmpty(Mono.defer(() -> Flux.fromIterable(permissionsByName.values())
						.filter(higherPerm -> higherPerm.level < perm.level)
						.flatMap(higherPerm -> higherPerm.checker.apply(ctx))
						.any(isGranted -> isGranted)));
		
	}
	
	private static class Permission {
		private final int level;
		private final Function<Context, Mono<Boolean>> checker;
		
		Permission(int level, Function<Context, Mono<Boolean>> checker) {
			this.level = level;
			this.checker = checker;
		}
	}
}
