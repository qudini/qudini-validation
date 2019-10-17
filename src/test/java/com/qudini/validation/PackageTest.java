package com.qudini.validation;

import org.junit.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PackageTest {

    private final static Validator<Void> testSuccessfulValidator = Validator
            .nested()
            .check(Validators.allowAll())
            .check(Validate.nonBlankCharSequence(), "abc")
            .check(Validators.nonNull(), new Object())

            // From https://fakenumber.org/generator/mobile
            .check(Validate.laxPhoneNumber(), "+447700900584")

            .check(Validate.constantCased(), "CONSTANT_CASED_STRING")
            .check(Validators.all(Validators.nullable(Validate.nonBlankCharSequence())), asList("abc", null, "def"))
            .check(Validate.isTrue, true)
            .check(
                    Validate.minimumLength(6).map(Account::getName),
                    new Account("test user name")
            )
            .check(Validators.nullable(Validate.minimumSize(3)), asList(1, 2, 3, 4))
            .check("must be abc", "^abc$", "abc")
            .check("must end with defabc", Pattern.compile("\\d+ defabc$"), "123456 defabc")
            .check(
                    Validate
                            .nonBlankCharSequence()
                            .and("must be more than 3 chars", x -> 3 < x.length()),
                    "abcdefghi"
            )
            .check(Validate.id(), 12L);

    private final static Validator<Void> testFailingValidator = Validator
            .nested()
            .check(Validators.any(Validate.isTrue), asList(false, false, false))
            .check(Validators.all(Validate.id()), asList(12L, 32L, 199L, -1L, 23L, 23L))
            .check(Validators.all(Validators.optional(Validate.id())), asList(
                    Optional.of(12L),
                    Optional.empty(),
                    Optional.of(-1L))
            )
            .check(
                    Validate.minimumLength(40).map(Account::getName),
                    new Account("test user name")
            )
            .check("must be abc", "^abc$", "")
            .check("must end with defabc", Pattern.compile("defabc$"), "123456")
            .check(Validate.minimumSize(4), null)
            .check(Validate.minimumSize(4), asList(1, 2, 3));

    @Test
    public void testThrowing() {
        try {
            Validators.throwing().check(testSuccessfulValidator).finish();
        } catch (final Validators.Exception exception) {
            fail("validation exception thrown during 'successful' validation: " + exception);
        }

        try {
            Validators.throwing().check(testFailingValidator).finish();
        } catch (final Validators.Exception exception) {
            return;
        }
        fail("validation exception was not thrown during a 'failing' validation");
    }

    @Test
    public void testAsserting() {
        try {
            Validators.asserting().check(testSuccessfulValidator).finish();
        } catch (final AssertionError exception) {
            fail("assertion error thrown during 'successful' validation: " + exception);
        }

        try {
            Validators.asserting().check(testFailingValidator).finish();
        } catch (final AssertionError exception) {
            return;
        }
        fail("assertion error was not thrown during a 'failing' validation");
    }

    @Test
    public void testReturning() {
        assertTrue(Validators.bool().check(testSuccessfulValidator).then(true));
        assertFalse(Validators.bool().check(testFailingValidator).then(true));
    }

    @Test
    public void testShortCircuiting() {
        final Validator.DescribedPredicate<Boolean> willShortCircuitIfFalse = Validator.DescribedPredicate.shortCircuit(
                "Short Circuit",
                bool -> bool
        );

        final Validator<Void> testShortCircuitingValidator = Validator
                .nested()
                .check(Validate.nonBlankCharSequence(), "")
                .check(Validators.nonNull(), null)

                // This will short circuit and the validations after this one will be ignored
                .check(willShortCircuitIfFalse, false)

                .check(Validate.constantCased(), "not_CONSTANT_CASED_STRING")
                .check(Validate.isTrue, false);

        try {
            Validators.throwing().check(testShortCircuitingValidator).finish();
            fail("Validation should not have been successful.");

        } catch (final Validators.Exception exception) {
            assertEquals(exception.collectInvalidValues().size(), 3);
        }
    }

    @Test
    public void testNestedShortCircuiting() {
        final Validator.DescribedPredicate<Boolean> willShortCircuitIfFalse = Validator.DescribedPredicate.shortCircuit(
                "Short Circuit",
                bool -> bool
        );

        final Validator<Void> testShortCircuitingValidator = Validator
                .nested()
                .check(Validate.nonBlankCharSequence(), "")
                .check(Validators.nonNull(), null)

                .check(Validator.nested()
                        .check(Validate.nonBlankCharSequence(), "")
                        // This will short circuit and the validations after this one will be ignored
                        .check(willShortCircuitIfFalse, false)
                        .check(Validators.nonNull(), null)
                )

                // The checks in the parent validator are not affected
                .check(Validate.constantCased(), "not_CONSTANT_CASED_STRING")
                .check(Validate.isTrue, false);

        try {
            Validators.throwing().check(testShortCircuitingValidator).finish();
            fail("Validation should not have been successful.");

        } catch (final Validators.Exception exception) {
            assertEquals(exception.collectInvalidValues().size(), 6);
        }
    }

    private static class Account {
        private String name;

        Account(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }
}
