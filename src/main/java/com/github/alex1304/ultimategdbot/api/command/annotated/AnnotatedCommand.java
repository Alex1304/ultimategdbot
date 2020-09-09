package com.github.alex1304.ultimategdbot.api.command.annotated;

import static reactor.function.TupleUtils.function;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.github.alex1304.ultimategdbot.api.Translator;
import com.github.alex1304.ultimategdbot.api.command.Command;
import com.github.alex1304.ultimategdbot.api.command.CommandDocumentation;
import com.github.alex1304.ultimategdbot.api.command.CommandDocumentationEntry;
import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.FlagInformation;
import com.github.alex1304.ultimategdbot.api.command.PermissionChecker;
import com.github.alex1304.ultimategdbot.api.command.PermissionDeniedException;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.Scope;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.ParamConversionException;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;

/**
 * Command implemented via annotations.
 */
public final class AnnotatedCommand implements Command {
	
	private static final Logger LOGGER = Loggers.getLogger(AnnotatedCommand.class);
	
	private final Object obj;
	private final Function<Context, Mono<Void>> action;
	private final Set<String> aliases;
	private final Function<Locale, CommandDocumentation> doc;
	private final String requiredPermission;
	private final PermissionLevel requiredPermissionLevel;
	private final Scope scope;


	private AnnotatedCommand(Object obj, Function<Context, Mono<Void>> action, Set<String> aliases,
			Function<Locale, CommandDocumentation> doc, String requiredPermission, PermissionLevel requiredPermissionLevel, Scope scope) {
		this.obj = obj;
		this.action = action;
		this.aliases = aliases;
		this.doc = doc;
		this.requiredPermission = requiredPermission;
		this.requiredPermissionLevel = requiredPermissionLevel;
		this.scope = scope;
	}

	@Override
	public Mono<Void> run(Context ctx) {
		return action.apply(ctx);
	}

	@Override
	public Set<String> getAliases() {
		return aliases;
	}
	
	@Override
	public CommandDocumentation getDocumentation(Locale locale) {
		return doc.apply(locale);
	}

	@Override
	public String getRequiredPermission() {
		return requiredPermission;
	}
	
	@Override
	public PermissionLevel getMinimumPermissionLevel() {
		return requiredPermissionLevel;
	}

	@Override
	public Scope getScope() {
		return scope;
	}
	
	@Override
	public String toString() {
		return "AnnotatedCommand{obj=" + obj.toString() + "}";
	}
	
	public static AnnotatedCommand from(Object obj, CommandProvider provider, PermissionChecker permChecker) {
		var cmdDescriptorAnnot = readCommandSpecAnnotation(obj);
		var cmdPermAnnot = obj.getClass().getAnnotation(CommandPermission.class);
		Method mainMethod = null;
		var subMethods = new HashMap<String, Method>();
		var mhCache = new HashMap<Method, MethodHandle>();
		for (var method : obj.getClass().getMethods()) {
			method.setAccessible(true);
			var cmdActionAnnot = method.getAnnotation(CommandAction.class);
			if (cmdActionAnnot == null) {
				continue;
			}
			validateMethodPrototype(method);
			if (cmdActionAnnot.value().isEmpty()) {
				if (mainMethod != null) {
					throw new InvalidAnnotatedObjectException("Duplicate action declaration");
				}
				mainMethod = method;
			} else {
				if (subMethods.containsKey(cmdActionAnnot.value())) {
					throw new InvalidAnnotatedObjectException("Duplicate subcommand declaration for '" + cmdActionAnnot.value() + "'");
				}
				subMethods.put(cmdActionAnnot.value(), method);
			}
		}
		if (mainMethod == null && subMethods.isEmpty()) {
			throw new InvalidAnnotatedObjectException("No action defined for the command");
		}
		var mainMethodOptional = Optional.ofNullable(mainMethod);
		try {
			if (mainMethod != null) {
				mhCache.put(mainMethod, MethodHandles.publicLookup().unreflect(mainMethod));
			}
			for (Method method : subMethods.values()) {
				mhCache.put(method, MethodHandles.publicLookup().unreflect(method));
			}
		} catch (IllegalAccessException e) {
			throw Exceptions.propagate(e);
		}
		return new AnnotatedCommand(obj,
				ctx -> {
					var args = ctx.args();
					var firstArgIndex = new AtomicInteger(2);
					var matchingMethod = Optional.ofNullable(args.tokenCount() > 1 ? subMethods.get(args.get(1)) : null)
							.or(() -> {
								firstArgIndex.set(1);
								return mainMethodOptional;
							});
					var invalidSyntax = new CommandFailedException(ctx.translate("CommonStrings", "invalid_syntax",
							ctx.prefixUsed() + "help " + args.get(0)));
					return Mono.justOrEmpty(matchingMethod)
							.switchIfEmpty(Mono.error(invalidSyntax))
							.filterWhen(method -> isSubcommandGranted(method, ctx, permChecker))
							.switchIfEmpty(Mono.error(new PermissionDeniedException()))
							.flatMap(method -> {
								LOGGER.debug("Matching method: {}#{}", method.getDeclaringClass().getName(), method.getName());
								var parameters = Flux.fromArray(method.getParameters()).skip(1);
								var argTokens = Flux.fromIterable(args.getTokens(method.getParameters().length + firstArgIndex.get() - 1))
										.skip(firstArgIndex.get());
								return Flux.zip(parameters, argTokens)
										.concatMap(function((param, arg) -> provider.convertParam(ctx, arg, param.getType())
												.onErrorMap(e -> new ParamConversionException(ctx, formatParamName(param.getName()), arg, e.getMessage()))))
										.collectList()
										.defaultIfEmpty(List.of())
										.map(argList -> new ArrayList<Object>(argList))
										.doOnNext(argList -> {
											argList.add(0, ctx);
											// Filling missing arguments with null, or throw an
											// exception if args aren't marked as @Nullable
											while (argList.size() < method.getParameters().length) {
												if (!method.getParameters()[argList.size()].isAnnotationPresent(Nullable.class)) {
													throw invalidSyntax;
												}
												argList.add(null);
											}
										})
										.flatMap(argList -> {
											try {
												var mh = mhCache.get(method);
												mh = mh.asType(mh.type().generic()).asSpreader(Object[].class, argList.size());
												if (Modifier.isStatic(method.getModifiers())) {
													return Mono.when((Publisher<?>) mh.invoke(argList.toArray()));
												} else {
													return Mono.when((Publisher<?>) mh.invoke(obj, argList.toArray()));
												}
											} catch (Throwable t) {
												throw Exceptions.propagate(t);
											}
										});
							})
							.then();
				},
				Set.of(cmdDescriptorAnnot.aliases()),
				locale -> buildDocumentation(() -> locale, cmdDescriptorAnnot.shortDescription(), mainMethodOptional.orElse(null), subMethods),
				cmdPermAnnot != null ? cmdPermAnnot.name() : "",
				cmdPermAnnot != null ? cmdPermAnnot.level() : PermissionLevel.PUBLIC,
				cmdDescriptorAnnot.scope());
	}

	private static Mono<Boolean> isSubcommandGranted(Method method, Context ctx, PermissionChecker permissionChecker) {
		var methodPermAnnot = method.getAnnotation(CommandPermission.class);
		if (methodPermAnnot == null) {
			return Mono.just(true);
		}
		return Mono.zip(permissionChecker.isGranted(methodPermAnnot.name(), ctx),
						permissionChecker.isGranted(methodPermAnnot.level(), ctx))
				.map(function(Boolean::logicalAnd));
				
	}

	private static CommandDescriptor readCommandSpecAnnotation(Object obj) {
		var cmdSpecAnnot = obj.getClass().getAnnotation(CommandDescriptor.class);
		if (cmdSpecAnnot == null) {
			throw new InvalidAnnotatedObjectException("@CommandSpec annotation is missing");
		}
		if (cmdSpecAnnot.aliases().length == 0) {
			throw new InvalidAnnotatedObjectException("@CommandSpec does not define any alias for the command");
		}
		return cmdSpecAnnot;
	}
	
	private static void validateMethodPrototype(Method method) {
		if (!Publisher.class.isAssignableFrom(method.getReturnType())) {
			throw new InvalidAnnotatedObjectException("The return type of a command action method must be compatible "
					+ "with the type " + Publisher.class.getName());
		}
		var paramTypes = method.getParameterTypes();
		if (paramTypes.length == 0 || paramTypes[0] != Context.class) {
			throw new InvalidAnnotatedObjectException("The first parameter of a command action method must be of type "
					+ Context.class.getName());
		}
	}
	
	private static CommandDocumentation buildDocumentation(Translator tr, String shortDescription, Method mainMethod, Map<String, Method> subMethods) {
		var methodsToProcess = new HashMap<String, Method>(subMethods);
		if (mainMethod != null) {
			methodsToProcess.put("", mainMethod);
		}
		var isHidden = methodsToProcess.values().stream().anyMatch(m -> m.getDeclaringClass().isAnnotationPresent(HiddenCommand.class));
		var docEntries = new HashMap<String, CommandDocumentationEntry>();
		methodsToProcess.forEach((name, method) -> {
			var syntax = Arrays.stream(method.getParameters())
					.skip(1)
					.map(param -> Tuples.of(formatParamName(param.getName()), param.isAnnotationPresent(Nullable.class)))
					.map(function((paramName, isNullable) -> isNullable ? "[" + paramName + "]" : "<" + paramName + ">"))
					.collect(Collectors.joining(" "));
			var cmdDocAnnot = method.getAnnotation(CommandDoc.class);
			var description = cmdDocAnnot == null ? "" : cmdDocAnnot.value();
			var flagInfo = new HashMap<String, FlagInformation>();
			var flagDocAnnot = method.getAnnotation(FlagDoc.class);
			if (flagDocAnnot != null) {
				for (var flag : flagDocAnnot.value()) {
					flagInfo.put(flag.name(), new FlagInformation(tr, flag.valueFormat(), flag.description()));
				}
			}
			docEntries.put(name, new CommandDocumentationEntry(tr, syntax, description, flagInfo));
		});
		return new CommandDocumentation(tr, shortDescription, docEntries, isHidden);
	}
	
	private static String formatParamName(String paramName) {
		var sb = new StringBuilder();
		for (var c : paramName.toCharArray()) {
			if (Character.isUpperCase(c)) {
				sb.append('_').append("" + Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
