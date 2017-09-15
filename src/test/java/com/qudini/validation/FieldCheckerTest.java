package com.qudini.validation;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FieldCheckerTest {

    private static final Account account = new Account("FirstName", "LastName", 32);

    @Test
    public void testGetter() {
        FieldChecker<Account, String> checker = new FieldChecker<>(
                Account.class,
                "lastName",
                "empty",
                (String x) -> !x.isEmpty()
        );
        assertTrue(checkAll(checker, account));
    }

    @Test
    public void testField() {
        FieldChecker<Account, String> checker = new FieldChecker<>(
                Account.class,
                "firstName",
                "empty",
                (String x) -> !x.isEmpty()
        );
        assertTrue(checkAll(checker, account));
    }

    @Test
    public void testCustomFieldLookup() {
        FieldChecker<Account, Integer> checker = new FieldChecker<>(
                Account.class,
                "firstName",
                Account::getAgeInYears,
                "negative",
                x -> 0 <= x
        );
        assertTrue(checkAll(checker, account));
    }

    private <Type, Field> boolean checkAll(FieldChecker<Type, Field> checker, Type object) {
        return checker
                .getPredicates()
                .stream()
                .allMatch(p -> p.test(checker.getGetter().apply(object)));
    }

    private static final class Account {
        public final String firstName;
        private final String lastName;
        private final int ageInYears;

        Account(String firstName, String lastName, int ageInYears) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.ageInYears = ageInYears;
        }

        public String getLastName() {
            return lastName;
        }

        public int getAgeInYears() {
            return ageInYears;
        }
    }
}

