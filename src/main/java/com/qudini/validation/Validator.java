package com.qudini.validation;

import com.google.common.collect.ImmutableList;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Provides a validator that yields either a user-defined response or a error creator that can receive all of the fields
 * that failed to validate properly. All predicates are checked even after initial failures, to make the error as
 * complete and descriptive as possible.
 * <p>
 * A validator check can be a predicate, like those in <code>helpers.validation.Validators</code>, a regexp as a string,
 * or a regexp pattern i.e. java.util.regex.Pattern. It can also be another validator, in which case they are combined.
 */
@ParametersAreNonnullByDefault
public final class Validator<A> {
    private final boolean wasInvalidated;
    private final boolean wasShortCircuited;
    private final ImmutableList<InvalidValue<?>> invalidValues;
    private final Function<Stream<InvalidValue<?>>, A> invalidResultCreator;

    /**
     * Creates a new validator that specifies how to create invalid results from an error string.
     */
    public Validator(Function<Stream<InvalidValue<?>>, A> invalidResultCreator) {
        this(false, false, ImmutableList.of(), invalidResultCreator);
    }

    private Validator(
            final boolean wasInvalidated,
            final boolean wasShortCircuited,
            final ImmutableList<InvalidValue<?>> invalidValues,
            final Function<Stream<InvalidValue<?>>, A> invalidResultCreator
    ) {
        this.wasInvalidated = wasInvalidated;
        this.wasShortCircuited = wasShortCircuited;
        this.invalidValues = invalidValues;
        this.invalidResultCreator = invalidResultCreator;
    }

    /**
     * Creates a nested validator, i.e. one that validates but does not yield an overall end result; that is handled by
     * whatever validator is at the top of the 'nesting'.
     */
    @CheckReturnValue
    @Nonnull
    public static Validator<Void> nested() {
        return new Validator<>(x -> {
            throw new IllegalStateException(
                    "a nested validator can only be nested; only a top-level validator may generate a result with " +
                            "#then or #finish"
            );
        });
    }

    /**
     * Validate with a described predicate.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B> Validator<A> check(final DescribedPredicate<B> describedPredicate, @Nullable final B x) {
        return check(describedPredicate, x, Function.identity());
    }

    /**
     * Validate with a described predicate.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B> Validator<A> check(
            final DescribedPredicate<B> describedPredicate,
            @Nullable final B x,
            final Function<B, B> applyFailingValue
    ) {
        return check(describedPredicate, Function.identity(), x, applyFailingValue);
    }

    /**
     * Validate with a described predicate.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B, F> Validator<A> check(
            final DescribedPredicate<F> describedPredicate,
            final Function<B, F> fieldMapper,
            @Nullable final B x
    ) {
        return check(describedPredicate, fieldMapper, x, Function.identity());
    }

    /**
     * Validate with a described predicate.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B> Validator<A> check(final String failureMessage, final Predicate<B> predicate, @Nullable final B x) {
        return check(DescribedPredicate.create(failureMessage, predicate), x);
    }

    /**
     * Validate a CharSequence with a regular expression.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B extends CharSequence> Validator<A> check(
            final String failureMessage,
            final Pattern pattern,
            @Nullable final B x
    ) {
        return check(DescribedPredicate.create(failureMessage, pattern), x);
    }

    /**
     * Validate a CharSequence with a regular expression.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B extends CharSequence> Validator<A> check(
            final String failureMessage,
            final Pattern pattern,
            @Nullable final B x,
            Function<B, B> applyFailingValue
    ) {
        return check(DescribedPredicate.create(failureMessage, pattern), x, applyFailingValue);
    }

    /**
     * Validate a CharSequence with a regular expression in a string.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B extends CharSequence> Validator<A> check(
            final String failureMessage,
            final String regexp,
            @Nullable final B x
    ) {
        return check(DescribedPredicate.create(failureMessage, regexp), x);
    }

    /**
     * Validate a CharSequence with a regular expression in a string.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    public <B extends CharSequence> Validator<A> check(
            final String failureMessage,
            final String regexp,
            @Nullable final B x,
            Function<B, B> applyFailingValue
    ) {
        return check(DescribedPredicate.create(failureMessage, regexp), x, applyFailingValue);
    }

    /**
     * Validate with a nested validator; this can be use several times to make a validators from several sub-validators.
     */
    @CheckReturnValue
    @Nonnull
    public Validator<A> check(final Validator<?> validator) {
        if (wasShortCircuited) {
            return this;

        } else {
            return new Validator<>(
                    wasInvalidated || validator.wasInvalidated,
                    false,
                    ListUtilities.concat(invalidValues, validator.invalidValues),
                    invalidResultCreator
            );
        }
    }

    /**
     * Upon validation success, return the result of invoking <code>f</code>. Otherwise, return the error constructed
     * with the function specified during construction.
     */
    public A then(final Supplier<A> f) {
        if (!wasInvalidated) {
            return f.get();
        }

        if (invalidValues.isEmpty()) {
            throw new IllegalStateException("invalidated validator has no recorded failures");
        }

        return invalidResultCreator.apply(invalidValues.stream());
    }

    /**
     * Upon validation success, return <code>result</code>. Otherwise, return the error constructed with the function
     * specified during construction.
     */
    @Nonnull
    public A then(@Nullable final A result) {
        return then(() -> result);
    }

    /**
     * Upon validation success, run <code>runnable</code>. This relies on the invalid result creator invoking
     * side-effects to distinguish itself from success; the obvious one is throwing an exception.
     */
    public void then(final Runnable runnable) {
        then(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Upon validation success do nothing. This relies on the invalid result creator invoking side-effects to
     * distinguish itself from success; the obvious one is throwing an exception.
     * <p>
     * <em>Unless you are relying on exceptions or other execution-flow-altering mechanisms, this is almost certainly
     * not what you are looking for</em>; use <code>Validator#then(Supplier)</code> or
     * <code>Validator#then(A)</code> instead.
     */
    public void finish() {
        then(() -> {
        });
    }


    /**
     * Validate with a described predicate.
     *
     * @see DescribedPredicate
     */
    @CheckReturnValue
    @Nonnull
    private <B, F> Validator<A> check(
            final DescribedPredicate<F> describedPredicate,
            final Function<B, F> fieldMapper,
            @Nullable final B x,
            final Function<B, B> applyFailingValue
    ) {
        if (wasShortCircuited) {
            return this;

        } else {
            final F fieldValue = fieldMapper.apply(x);
            final boolean valid = describedPredicate.getPredicate().test(fieldValue);

            final ImmutableList<InvalidValue<?>> newInvalidValues = valid
                    ? invalidValues
                    : ListUtilities.cons(
                    new InvalidValue<>(describedPredicate.describe(), fieldValue),
                    invalidValues
            );

            return new Validator<>(
                    (wasInvalidated || !valid),
                    (!valid && describedPredicate.shouldShortCircuit()),
                    newInvalidValues,
                    invalidResultCreator
            );
        }
    }

    /**
     * A predicate for validation purposes; these should not be used inline much, but instead accumulated in collections
     * of common predicates for values in a particular domain.
     * <p>
     * Upon failure, they offer failure descriptions. It's up to the API user whether they're human-readable or symbolic
     * for client-side code to handle.
     */
    @CheckReturnValue
    @ParametersAreNonnullByDefault
    public static class DescribedPredicate<B> {

        private final String failureMessage;
        private final Predicate<B> predicate;
        private final boolean shouldShortCircuit;

        private DescribedPredicate(
                final String failureMessage,
                final Predicate<B> predicate,
                final boolean shouldShortCircuit
        ) {
            this.failureMessage = failureMessage;
            this.predicate = predicate;
            this.shouldShortCircuit = shouldShortCircuit;
        }

        /**
         * Create a described predicate from a description and a predicate.
         */
        @Nonnull
        public static <C> DescribedPredicate<C> create(final String failureMessage, final Predicate<C> predicate) {
            return new DescribedPredicate<>(failureMessage, predicate, false);
        }

        /**
         * Create a described predicate from a description and a regular expression pattern.
         */
        @Nonnull
        public static <C extends CharSequence> DescribedPredicate<C> create(
                final String description,
                final Pattern pattern
        ) {
            return Validators.nonNullable(create(description, x -> pattern.matcher(x).matches()));
        }

        /**
         * Create a described predicate from a description and a regular expression pattern in a string.
         */
        @Nonnull
        public static <C extends CharSequence> DescribedPredicate<C> create(
                final String description,
                final String pattern
        ) {
            return create(description, Pattern.compile(pattern));
        }

        /**
         * Create a described predicate from a description and a predicate, and states its intent to be
         * short-circuited by combining mechanisms like validators.
         */
        @Nonnull
        public static <C> DescribedPredicate<C> shortCircuit(final String description, final Predicate<C> predicate) {
            return new DescribedPredicate<>(description, predicate, true);
        }

        /**
         * Create a described predicate from a description and a regular expression pattern, and states its intent to
         * be short-circuited by combining mechanisms like validators.
         */
        @Nonnull
        public static <C extends CharSequence> DescribedPredicate<C> shortCircuit(
                final String description,
                final Pattern pattern
        ) {
            return Validators.nonNullable(shortCircuit(description, x -> pattern.matcher(x).matches()));
        }

        /**
         * Create a described predicate from a description and a regular expression pattern in a string, and states
         * its intent to be short-circuited by combining mechanisms like validators.
         */
        @Nonnull
        public static <C extends CharSequence> DescribedPredicate<C> shortCircuit(
                final String description,
                final String pattern
        ) {
            return shortCircuit(description, Pattern.compile(pattern));
        }

        /**
         * Get the description, which should indicate to an end-user why the validation did not work.
         */
        @Nonnull
        public String describe() {
            return failureMessage;
        }

        /**
         * Get the predicate which should be able to validate input according to the description given by
         * <code>Validator.DescribedPredicate#getDescription</code>.
         */
        @Nonnull
        public Predicate<B> getPredicate() {
            return predicate;
        }

        /**
         * <code>and</code> as you'd expect it, except that it also combines the descriptions with " and ". This is
         * <em>always</em> short-circuited, as it delegates to {@link java.util.function.Predicate#and}.
         */
        @Nonnull
        public DescribedPredicate<B> and(final DescribedPredicate<B> that) {
            return DescribedPredicate.create(
                    describe() + " and " + that.describe(),
                    getPredicate().and(that.getPredicate())
            );
        }

        /**
         * <code>and</code> as you'd expect it, except that it also combines the descriptions with " and ". This is
         * <em>always</em> short-circuited, as it delegates to {@link java.util.function.Predicate#and}.
         */
        @Nonnull
        public DescribedPredicate<B> and(final String failureMessage, final Predicate<B> predicate) {
            return and(create(failureMessage, predicate));
        }

        /**
         * <code>or</code> as you'd expect it, except that it also combines the descriptions with " or ". This is
         * <em>always</em> short-circuited, as it delegates to {@link java.util.function.Predicate#or}.
         */
        @Nonnull
        public DescribedPredicate<B> or(final DescribedPredicate<B> that) {
            return DescribedPredicate.create(
                    describe() + " or " + that.describe(),
                    getPredicate().or(that.getPredicate())
            );
        }

        /**
         * <code>or</code> as you'd expect it, except that it also combines the descriptions with " or ". This is
         * <em>always</em> short-circuited, as it delegates to {@link java.util.function.Predicate#or}.
         */
        @Nonnull
        public DescribedPredicate<B> or(final String failureMessage, final Predicate<B> predicate) {
            return or(create(failureMessage, predicate));
        }

        /**
         * Create a predicate for type {@code B}, mapped from {@code B} by {@code f}.
         * <pre>
         * {@code
         *
         * class Account {
         *     private String name;
         *     Account(String name) { this.name = name; }
         *     String getName() { return name; }
         * }
         *
         * [...]
         *
         * .check(
         *         Validate.minimumLength(6).map(Account::getName),
         *         new Account("test user name"))
         * }
         * </pre>
         */
        @Nonnull
        public <C> DescribedPredicate<C> map(Function<C, B> f) {
            return new DescribedPredicate<>(describe(), x -> test(f.apply(x)), shouldShortCircuit());
        }

        /**
         * Test the underlying predicate directly.
         */
        public boolean test(B value) {
            return predicate.test(value);
        }

        /**
         * Return a short-circuited version of {@code this}.
         */
        public DescribedPredicate<B> shortCircuit() {
            return DescribedPredicate.shortCircuit(describe(), getPredicate());
        }

        /**
         * Should this predicate be short-circuited? This is merely advisory; abstractions using described predicates
         * can choose how to interpret the request.
         */
        public boolean shouldShortCircuit() {
            return shouldShortCircuit;
        }
    }

    /**
     * An invalid value, along with a description of why it is incorrect.
     */
    @CheckReturnValue
    @ParametersAreNonnullByDefault
    public static class InvalidValue<B> {
        private final String failureMessage;
        private final B value;

        public InvalidValue(final String failureMessage, @Nullable final B value) {
            this.failureMessage = failureMessage;
            this.value = value;
        }

        @Nonnull
        public String getDescription() {
            return failureMessage;
        }

        public B getValue() {
            return value;
        }
    }

}
