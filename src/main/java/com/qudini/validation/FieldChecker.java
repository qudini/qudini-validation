package com.qudini.validation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

@ParametersAreNonnullByDefault
final class FieldChecker<Type, Field> {
    private final String fieldName;
    private final Function<Type, Field> getter;
    private final Set<Validator.DescribedPredicate<Field>> predicates;

    FieldChecker(Class<Type> klass, String fieldName, Set<Validator.DescribedPredicate<Field>> predicates) {
        this(klass, fieldName, lookupField(klass, fieldName), predicates);
    }

    FieldChecker(
            Class<Type> klass,
            String fieldName,
            Function<Type, Field> getter,
            Set<Validator.DescribedPredicate<Field>> predicates
    ) {
        this.getter = getter;
        this.fieldName = fieldName;
        this.predicates = unmodifiableSet(predicates);
    }

    @SafeVarargs
    FieldChecker(Class<Type> klass, String fieldName, Validator.DescribedPredicate<Field>... predicates) {
        this(klass, fieldName, lookupField(klass, fieldName), new HashSet<>(asList(predicates)));
    }

    @SafeVarargs
    FieldChecker(
            Class<Type> klass,
            String fieldName,
            Function<Type, Field> getter,
            Validator.DescribedPredicate<Field>... predicates
    ) {
        this(klass, fieldName, getter, new HashSet<>(asList(predicates)));
    }

    FieldChecker(Class<Type> klass, String fieldName, String failureMessage, Predicate<Field> predicate) {
        this(
                klass,
                fieldName,
                lookupField(klass, fieldName),
                Validator.DescribedPredicate.create(failureMessage, predicate)
        );
    }

    FieldChecker(
            Class<Type> klass,
            String fieldName,
            Function<Type, Field> getter,
            String failureMessage,
            Predicate<Field> predicate
    ) {
        this(
                klass,
                fieldName,
                getter,
                Validator.DescribedPredicate.create(failureMessage, predicate)
        );
    }

    private static <Type, Field> Function<Type, Field> lookupField(Class<Type> klass, String fieldName) {
        try {
            return lookupFieldGetter(klass, fieldName);
        } catch (NoSuchMethodException e) {
            return lookupFieldDirectly(klass, fieldName);
        }
    }

    private static <Type, Field> Function<Type, Field> lookupFieldGetter(Class<Type> klass, String fieldName)
            throws NoSuchMethodException {
        String getterSuffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method getter = klass.getDeclaredMethod("get" + getterSuffix);
        return x -> {
            try {
                return (Field) getter.invoke(x);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new GetterInvocationException(getter, e);
            }
        };
    }

    private static <Type, Field> Function<Type, Field> lookupFieldDirectly(Class<Type> klass, String fieldName) {
        final java.lang.reflect.Field field;
        try {
            field = klass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new FieldMissingException(fieldName);
        }
        return x -> {
            try {
                final Field value;
                boolean originallyAccessible = field.isAccessible();
                try {
                    field.setAccessible(true);
                    value = (Field) field.get(x);
                } finally {
                    if (!originallyAccessible) {
                        field.setAccessible(false);
                    }
                }
                return value;
            } catch (IllegalAccessException e) {
                throw new FieldMissingException(fieldName);
            }
        };
    }

    public String getFieldName() {
        return fieldName;
    }

    public Function<Type, Field> getGetter() {
        return getter;
    }

    public Set<Validator.DescribedPredicate<Field>> getPredicates() {
        return predicates;
    }
}
