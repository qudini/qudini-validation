package com.qudini.validation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@CheckReturnValue
@ParametersAreNonnullByDefault
final class ListUtilities {

    private ListUtilities() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static <E> ImmutableList<E> cons(final E head, final ImmutableList<E> tail) {
        return ImmutableList.copyOf(Iterables.concat(
                ImmutableList.of(head),
                tail
        ));
    }

    @Nonnull
    public static <E> ImmutableList<E> concat(
            final ImmutableList<E> listA,
            final ImmutableList<E> listB
    ) {
        return ImmutableList.copyOf(Iterables.concat(listA, listB));
    }
}
