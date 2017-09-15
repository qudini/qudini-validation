package com.qudini.validation;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <code>Validator</code> utilities. Designed to be used with <code>Validator</code> and <code>Validate</code> like so:
 * <pre>{@code
 * return Validators
 *         .optional()
 *         .check(Validate.bookingWidgetIdentifier, bwIdentifier)
 *         .check(Validate.storeID, storeId)
 *         .then(Optional.of(42));
 * }</pre>
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
public final class Validators {

    private Validators() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a validator that either runs the user-defined block or throws <code>Validators.Exception</code> upon
     * validation failure, detailing the accumulated validation errors.
     */
    @Nonnull
    public static Validator<Void> throwing() {
        return new Validator<>(errors -> {
            throw new Exception(errors);
        });
    }

    /**
     * Creates a validator that either runs the user-defined block or throws <code>AssertionError</code> upon validation
     * failure, detailing the accumulated validation errors and stating a validation error as the underlying cause.
     */
    @Nonnull
    public static Validator<Void> asserting() {
        return new Validator<>(errors -> {
            final List<Validator.InvalidValue<?>> collected = errors.collect(Collectors.toList());

            throw new AssertionError(
                    validationErrorsToString(collected.stream()),
                    new Exception(collected.stream())
            );
        });
    }

    /**
     * Creates a validator that returns a value indicating validation success or failure; accumulated validation
     * messages are discarded.
     */
    @Nonnull
    public static <A> Validator<A> returning(final A x) {
        return new Validator<>(y -> x);
    }

    /**
     * Creates a validator that returns an empty <code>java.util.Optional</code> value upon failure or a user-defined
     * <code>java.util.Optional</code> upon success, presumably a non-empty one.
     */
    @Nonnull
    public static <A> Validator<Optional<A>> optional() {
        return returning(Optional.empty());
    }

    /**
     * Creates a validator that returns a boolean indicating validation success or failure; accumulated validation
     * messages are discarded.
     */
    @Nonnull
    public static Validator<Boolean> bool() {
        return returning(false);
    }

    public static Validator<Void> allowAll() {
        return Validator.nested().check("", x -> true, 42);
    }

    /**
     * Add null-tolerance to a validator.
     */
    @Nonnull
    public static <A> Validator.DescribedPredicate<A> nullable(final Validator.DescribedPredicate<A> predicate) {
        final Validator.DescribedPredicate<A> isNull = isNull();
        return isNull.or(predicate);
    }

    /**
     * Add null-intolerance to a validator.
     */
    @Nonnull
    public static <A> Validator.DescribedPredicate<A> nonNullable(final Validator.DescribedPredicate<A> predicate) {
        final Validator.DescribedPredicate<A> nonNull = nonNull();
        return nonNull.and(predicate);
    }

    /**
     * Is null? (May require an explicit type parameter in complex expressions.)
     */
    @Nonnull
    public static <A> Validator.DescribedPredicate<A> isNull() {
        return Validator.DescribedPredicate.create("nulls are allowed", Objects::isNull);
    }

    /**
     * Is null? (May require an explicit type parameter in complex expressions.)
     */
    @Nonnull
    public static <A> Validator.DescribedPredicate<A> nonNull() {
        return Validator.DescribedPredicate.create("a non-null object is required", Objects::nonNull);
    }

    /**
     * Add optionality-unwrapping to a validator; empty optional values and non-empty values failing the predicate cause
     * a invalidation.
     */
    @Nonnull
    public static <A> Validator.DescribedPredicate<Optional<A>> unwrappedOptional(
            final Validator.DescribedPredicate<A> predicate
    ) {
        return Validator.DescribedPredicate.create(
                predicate.describe() + " and must be non-empty java.util.Optional",
                x -> x.map(y -> predicate.getPredicate().test(y)).orElse(false)
        );
    }

    /**
     * Add optionality to a validator; empty optional values and non-empty options that pass the predicate are valid;
     * otherwise a invalidation occurs.
     */
    @Nonnull
    public static <A> Validator.DescribedPredicate<Optional<A>> optional(
            final Validator.DescribedPredicate<A> predicate
    ) {
        return Validator.DescribedPredicate.create(
                predicate.describe() + " or must be an empty java.util.Optional",
                x -> x.isPresent() ? x.map(y -> predicate.getPredicate().test(y)).orElse(false) : true
        );
    }

    /**
     * For type <code>A</code>, convert a validator of <code>A</code> into a validator of a stream of <code>A</code>.
     * Validation passes if all elements of the stream match the predicate.
     */
    @Nonnull
    public static <A, C extends Collection<A>> Validator.DescribedPredicate<C> all(
            final Validator.DescribedPredicate<A> predicate
    ) {
        return Validator.DescribedPredicate.create(
                predicate.describe() + "; one or more failed in sequence",
                xs -> xs.stream().allMatch(predicate.getPredicate())
        );
    }

    /**
     * For type <code>A</code>, convert a validator of <code>A</code> into a validator of a collection of
     * <code>A</code>. Validation passes if any element of the stream matches the predicate.
     */
    @Nonnull
    public static <A, C extends Collection<A>> Validator.DescribedPredicate<C> any(
            final Validator.DescribedPredicate<A> predicate
    ) {
        return Validator.DescribedPredicate.create(
                predicate.describe() + "; all failed in the sequence",
                xs -> xs.stream().anyMatch(predicate.getPredicate())
        );
    }

    /**
     * Convenience method for a common use case: one-shot validations that throw exceptions on failures. A shorthand
     * for:
     * <pre>
     * {@code Validators.throwing().check(predicate, value).finish();}
     * </pre>
     */
    public static <A> void require(final Validator.DescribedPredicate<A> predicate, final A value) {
        Validators.throwing().check(predicate, value).finish();
    }

    /**
     * Convenience method for a common use case: one-shot validations that throw exceptions on failures. A shorthand
     * for:
     * <pre>
     * {@code Validators.throwing().check(predicate, value).finish();}
     * </pre>
     */
    public static <A> void require(final Validator<A> validator) {
        Validators.throwing().check(validator).finish();
    }

    public static void invalidate(final String reason, final Object x) {
        throw new Exception(Stream.of(new Validator.InvalidValue<>(reason, x)));
    }

    private static String validationErrorsToString(final Stream<Validator.InvalidValue<?>> invalidValues) {
        return (
                "Invalid Values:\n" +

                        invalidValues
                                .map(x -> String.format(
                                        "\t%s: %s",
                                        x.getDescription(),
                                        x.getValue()
                                ))
                                .collect(Collectors.joining("\n"))
        );
    }

    /**
     * A failed validation, detailing each failed validation.
     */
    public static class Exception extends RuntimeException {
        private final transient List<Validator.InvalidValue<?>> invalidValues;

        public Exception(@Nonnull final Stream<Validator.InvalidValue<?>> invalidValues) {
            this.invalidValues = Collections.unmodifiableList(invalidValues.collect(Collectors.toList()));
        }

        @Override
        public String getMessage() {
            return validationErrorsToString(streamInvalidValues());
        }

        /**
         * Stream the failed values; each invocation gives a new stream.
         */
        @CheckReturnValue
        @Nonnull
        public Stream<Validator.InvalidValue<?>> streamInvalidValues() {
            return invalidValues.stream();
        }

        public Collection<Validator.InvalidValue<?>> collectInvalidValues() {
            return invalidValues;
        }
    }
}
