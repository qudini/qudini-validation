package com.qudini.validation;

import com.qudini.validation.Validator.DescribedPredicate;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CheckReturnValue
@ParametersAreNonnullByDefault
public final class Validate {

    public static final DescribedPredicate<Boolean> isTrue = DescribedPredicate.create("must be true", x -> x);

    /**
     * Is zero or more?
     */
    public static final DescribedPredicate<Integer> naturalInt = minimum(0);

    /**
     * Is zero or more?
     */
    public static final DescribedPredicate<Long> naturalLong = minimum(0L);

    private Validate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Is CONSTANT_CASED? Being blank also qualifies; combine with {@link #nonBlankCharSequence()} if that is not
     * desired.
     */
    @Nonnull
    public static <A extends CharSequence> DescribedPredicate<A> constantCased() {
        return DescribedPredicate.create(
                "must be a CONSTANT_CASED string",
                "^[A-Z_]*$"
        );
    }

    /**
     * Contains <code>x</code>, regardless of the case of either?
     */
    @Nonnull
    public static DescribedPredicate<String> doesNotContainCaseInsensitive(final String x) {
        final String lowerX = x.toLowerCase();
        return DescribedPredicate.create(
                String.format("must not contain '%s'", x),
                y -> !y.toLowerCase().contains(lowerX)
        );
    }

    /**
     * Contains characters other than whitespace or nothing?
     */
    @Nonnull
    public static <A extends CharSequence> DescribedPredicate<A> nonBlankCharSequence() {
        return DescribedPredicate.create(
                "a non-blank string is required",
                "^.*?\\S.*?"
        );
    }

    /**
     * Is an ID that could be used a a primary key in a database?
     */
    @Nonnull
    public static <N extends Number> DescribedPredicate<N> id() {
        return DescribedPredicate.create(
                "invalid ID; must be a positive number",
                x -> 0 <= x.longValue()
        );
    }

    /**
     * Is a customer identifier that has no obscure punctuation?
     */
    @Nonnull
    public static <A extends CharSequence> DescribedPredicate<A> name() {
        return DescribedPredicate.create(
                "invalid name; must not have obscure punctuation",
                "^[^<>$@^%!]+$"
        );
    }

    /**
     * Vaguely resembles a phone number? Useful for liberally accepting a number before getting more information such as
     * country code from the client.
     */
    @Nonnull
    public static <A extends CharSequence> DescribedPredicate<A> laxPhoneNumber() {
        return DescribedPredicate.create(
                "invalid phone number",
                "^\\+?\\d\\d\\d+$"
        );
    }

    /**
     * Is at least <code>n</code>?
     */
    @Nonnull
    public static <N extends Number> DescribedPredicate<N> minimum(final N n) {
        return DescribedPredicate.create(
                "must be at least " + n,
                x -> n.longValue() <= x.longValue()
        );
    }

    /**
     * Has at least <code>n</code> elements?
     */
    @Nonnull
    public static <A extends CharSequence> DescribedPredicate<A> minimumLength(final int n) {
        return Validators.nonNullable(DescribedPredicate.create(
                "must have at least " + n + " characters",
                x -> n <= x.length()
        ));
    }

    /**
     * Has at least <code>n</code> characters?
     */
    @Nonnull
    public static DescribedPredicate<char[]> minimumCharsLength(final int n) {
        return Validators.nonNullable(DescribedPredicate.create(
                "must have at least " + n + " elements",
                x -> n <= x.length
        ));
    }

    /**
     * Has at least <code>n</code> bytes?
     */
    @Nonnull
    public static DescribedPredicate<byte[]> minimumBytesLength(final int n) {
        return Validators.nonNullable(DescribedPredicate.create(
                "must have at least " + n + " elements",
                x -> n <= x.length
        ));
    }

    /**
     * Has at least <code>n</code> elements?
     */
    @Nonnull
    public static <A, C extends Collection<A>> DescribedPredicate<C> minimumSize(final int n) {
        return Validators.nonNullable(DescribedPredicate.create(
                "at least " + n + " elements must be present",
                xs -> n <= xs.size()
        ));
    }

    /**
     * Can be casted to <code>type</code>?
     */
    @Nonnull
    public static <A> DescribedPredicate<A> assignableTo(final Class<?> type) {
        return Validators.nonNullable(DescribedPredicate.create(
                "can not cast to" + type,
                x -> type.isAssignableFrom(x.getClass())
        ));
    }

    @Nonnull
    public static <E extends Enum<E>> DescribedPredicate<String> nameOfEnumVariant(final Class<E> enumClass) {

        final List<String> names = Stream
                .of(enumClass.getEnumConstants())
                .map(E::name)
                .collect(Collectors.toList());

        final String message = "value not in allowed values: " + Arrays.toString(names
                .stream()
                .toArray(String[]::new)
        );

        return DescribedPredicate.create(
                message,
                x -> names.stream().anyMatch(x::equals)
        );
    }

    @Nonnull
    public static <K, M extends Map<K, ?>> DescribedPredicate<K> containedWithin(final M map) {
        return DescribedPredicate.create(
                "must exist in map",
                map::containsKey
        );
    }

    /**
     * Checks the existence and types of values in a map; useful for checking JSON bodies passed in HTTP requests.
     */
    @Nonnull
    public static <M extends Map<String, ?>> DescribedPredicate<String> validTypedMapElement(
            final M map, final Class<?> klass) {
        return DescribedPredicate.create(
                "must be passed as a JSON property as a " + klass.getSimpleName(),
                key -> map.containsKey(key) && assignableTo(klass).getPredicate().test(map.get(key))
        );
    }
}
