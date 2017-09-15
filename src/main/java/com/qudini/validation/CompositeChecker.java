package com.qudini.validation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

@ParametersAreNonnullByDefault
public final class CompositeChecker<Type> {
    private final List<FieldChecker<Type, Object>> checkers;

    private CompositeChecker(List<FieldChecker<Type, Object>> checkers) {
        this.checkers = unmodifiableList(checkers);
    }

    public static <Type> Builder<Type> builder(Class<Type> klass) {
        return new Builder<>(klass);
    }

    public Set<FailedField> check(Type object) {
        return checkers
                .stream()
                .filter(checker -> checker
                        .getPredicates()
                        .stream()
                        .allMatch(p -> p.test(checker.getGetter().apply(object)))
                )
                .map(failed -> new FailedField(
                        failed.getFieldName(),
                        failed
                                .getPredicates()
                                .stream()
                                .map(Validator.DescribedPredicate::describe)
                                .collect(Collectors.toSet())
                ))
                .collect(Collectors.toSet());
    }

    public static final class Builder<Type> {
        private final Class<Type> klass;
        private final List<FieldChecker<Type, Object>> checkers = new LinkedList<>();

        Builder(Class<Type> klass) {
            this.klass = klass;
        }

        public Builder field(FieldChecker<Type, Object> checker) {
            checkers.add(checker);
            return this;
        }

        public Builder field(
                String fieldName,
                Validator.DescribedPredicate<Object>... predicates
        ) {
            return field(new FieldChecker<>(klass, fieldName, predicates));
        }

        public Builder field(
                String fieldName,
                Function<Type, Object> getter,
                Validator.DescribedPredicate<Object>... predicates
        ) {
            return field(new FieldChecker<>(klass, fieldName, getter, predicates));
        }

        public Builder field(
                String fieldName,
                String failureMessage,
                Predicate<Object> predicate
        ) {
            Validator.DescribedPredicate<Object> p = Validator.DescribedPredicate.create(failureMessage, predicate);
            return field(new FieldChecker<>(klass, fieldName, p));
        }

        public Builder field(
                String fieldName,
                Function<Type, Object> getter,
                String failureMessage,
                Predicate<Object> predicate
        ) {
            Validator.DescribedPredicate<Object> p = Validator.DescribedPredicate.create(failureMessage, predicate);
            return field(new FieldChecker<>(klass, fieldName, getter, p));
        }

        public CompositeChecker<Type> build() {
            return new CompositeChecker<>(checkers);
        }
    }
}