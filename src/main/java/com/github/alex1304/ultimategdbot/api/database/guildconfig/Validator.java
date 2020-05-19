package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import static java.util.function.Predicate.not;

import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * Validates a value in a reactive way.
 * 
 * @param <T> the type of value to validate
 */
@FunctionalInterface
public interface Validator<T> extends Function<T, Mono<T>> {
	
	/**
	 * Creates a validator that allows the value if BOTH this validator AND the
	 * given one allow the value. If this validator fails, the given one is not
	 * evaluated.
	 * 
	 * @param other the other validator
	 * @return a validator allowing the value if this validator AND the given one
	 *         allow it
	 */
	default Validator<T> and(Validator<T> other) {
		return value -> apply(value).flatMap(other);
	}
	
	/**
	 * Creates a validator that allows the value if EITHER this validator OR the
	 * given one allows the value. If this validator allows the value, the given one
	 * is not evaluated.
	 * 
	 * @param other the other validator
	 * @return a validator allowing the value if this validator OR the given one
	 *         allows it
	 */
	default Validator<T> or(Validator<T> other) {
		return value -> apply(value)
				.onErrorResume(ValidationException.class, e -> other.apply(value));
	}
	
	/**
	 * Creates a validator that allows all values.
	 * 
	 * @param <T> the type of value to validate
	 * @return a validator
	 */
	public static <T> Validator<T> allowingAll() {
		return Mono::justOrEmpty;
	}
	
	/**
	 * Creates a validator that denies all values.
	 * 
	 * @param <T>            the type of value to validate
	 * @param failureMessage the message to forward to the user if the validation
	 *                       fails
	 * @return a validator
	 */
	public static <T> Validator<T> denyingAll(String failureMessage) {
		return value -> Mono.error(new ValidationException(failureMessage));
	}

	/**
	 * Creates a validator that allows the value if the predicate is matched, with
	 * the given failure message if the validation doesn't pass.
	 * 
	 * @param <T>            the type of value to validate
	 * @param predicate      the predicate deciding whether to allow the value
	 * @param failureMessage the message to forward to the user if the validation
	 *                       fails
	 * @return a validator
	 */
	public static <T> Validator<T> allowingIf(Predicate<? super T> predicate,
			String failureMessage) {
		return value -> value == null ? Mono.empty() : Mono.just(value)
				.filter(predicate)
				.switchIfEmpty(Mono.error(new ValidationException(failureMessage)));
	}

	/**
	 * Creates a validator that denies the value if the predicate is matched, with
	 * the given failure message if the validation doesn't pass.
	 * 
	 * @param <T>            the type of value to validate
	 * @param predicate      the predicate deciding whether to deny the value
	 * @param failureMessage the message to forward to the user if the validation
	 *                       fails
	 * @return a validator
	 */
	public static <T> Validator<T> denyingIf(Predicate<? super T> predicate,
			String failureMessage) {
		return allowingIf(not(predicate), failureMessage);
	}
	
	/**
	 * Creates a validator that allows the value if the Publisher emits true, with
	 * the given failure message if the validation doesn't pass. The async predicate
	 * obeys the same rules as described in {@link Mono#filterWhen(Function)}
	 * 
	 * @param <T>            the type of value to validate
	 * @param asyncPredicate the async predicate deciding whether to allow the value
	 * @param failureMessage the message to forward to the user if the validation
	 *                       fails
	 * @return a validator
	 */
	public static <T> Validator<T> allowingWhen(Function<? super T, ? extends Publisher<Boolean>> asyncPredicate,
			String failureMessage) {
		return value -> value == null ? Mono.empty() : Mono.just(value)
				.filterWhen(asyncPredicate)
				.switchIfEmpty(Mono.error(new ValidationException(failureMessage)));
	}
	
	/**
	 * Creates a validator that denies the value if the Publisher emits true, with
	 * the given failure message if the validation doesn't pass. The async predicate
	 * obeys the same rules as described in {@link Mono#filterWhen(Function)}
	 * 
	 * @param <T>            the type of value to validate
	 * @param asyncPredicate the async predicate deciding whether to deny the value
	 * @param failureMessage the message to forward to the user if the validation
	 *                       fails
	 * @return a validator
	 */
	public static <T> Validator<T> denyingWhen(Function<? super T, ? extends Publisher<Boolean>> asyncPredicate,
			String failureMessage) {
		return value -> value == null ? Mono.empty() : Mono.just(value)
				.filterWhen(asyncPredicate)
				.flatMap(__ -> Mono.<T>error(new ValidationException(failureMessage)))
				.defaultIfEmpty(value);
	}
	
	/**
	 * Creates a validator that denies null values.
	 * 
	 * @param <T>            the type of value to validate
	 * @param failureMessage the message to forward to the user if the validation
	 *                       fails
	 * @return a validator
	 */
	public static <T> Validator<T> denyingIfNull(String failureMessage) {
		return value -> Mono.justOrEmpty(value)
				.switchIfEmpty(Mono.error(new ValidationException(failureMessage)));
	}
	
	/**
	 * Creates a validator that only allows null values.
	 * 
	 * @param <T>            the type of value to validate
	 * @param failureMessage the message to forward to the user if the validation
	 *                       fails
	 * @return a validator
	 */
	public static <T> Validator<T> denyingIfNotNull(String failureMessage) {
		return value -> Mono.justOrEmpty(value)
				.flatMap(__ -> Mono.<T>error(new ValidationException(failureMessage)))
				.defaultIfEmpty(value);
	}
}
