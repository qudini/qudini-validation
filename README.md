# Qudini Validation

[![CircleCI](https://circleci.com/gh/qudini/qudini-validation.svg?style=svg)](https://circleci.com/gh/qudini/qudini-validation)

Validate inputs using a compact API.

```java
import com.qudini.Validate;
import com.qudini.Validator;
import com.qudini.Validators;

class Main {

    public static void main() {

        // Successfully validate, avoiding the thrown exception.
        Validators
                .throwing()
                .check(Validate.nonBlankCharSequence(), "abc")
                .check(Validators.nonNull(), new Object())

                // From https://fakenumber.org/generator/mobile
                .check(Validate.laxPhoneNumber(), "+447700900584")
                .check(
                        Validate.minimumLength(5).map(Account::getName),
                        new Account("test user name")
                )
                .check(Validate.constantCased(), "CONSTANT_CASED_STRING")
                .check(
                        Validators.all(Validators.nullable(
                                Validate.nonBlankCharSequence())
                        ),
                        asList("abc", null, "def")
                )
                .check(Validate.isTrue, true)
                .check(Validate.id(), 12L)
                .finish();

        // Fail to validate, returning an empty Optional rather than the
        // specified 42.
        final Optional<Integer> result = Validators
                .optional()
                .check(
                        Validators.any(Validate.isTrue),
                        asList(false, false, false)
                )
                .check(
                        Validators.all(Validate.id()),
                        asList(12L, 32L, 199L, -1L, 23L, 23L)
                )
                .check(
                        Validators.all(Validators.optional(Validate.id())),
                        asList(
                                Optional.of(12L),
                                Optional.empty(),
                                Optional.of(-1L)
                        )
                )
                .check(Validate.minimumSize(4), asList(1, 2, 3))
                .then(Optional.of(42));

        // Bypass the validator features and use the underlying predicates
        // directly.
        boolean valid = Validate.minimumLength(40).test(account.getName());
    }

    Optional<Integer> maybeNaturalNumber(final int n) {
        return Validator.optional().check(Validate.naturalNumber()).then(n);
    }
}
```

Features:

* Compact notation without boilerplate.
* Provided validators for common cases: IDs, natural numbers, CONSTANT_CASED
  strings, non-blank strings, and minimum lengths.
* Strongly typed and immutable; no ClassCastExceptions and no unintended race
  conditions from sharing validators across threads.
* Composable and nestable; build validators for specific domains and compose
  them in top-level validators that respond to validation errors differently.
* Good support for modern, idiomatic Java; built-in support for
  `java.util.Optional` and tasteful use of lambdas.
* Validate as a side-effect via exceptions, or as values, such as
  `java.lang.Boolean` or a user-defined type like a 422 HTTP request object.
* Define your own validators, validator 'contexts' like HTTP or logging
  validators, and create 'base' validators that are embedded in other
  validators.

POJO validators are currently a beta feature under construction. They allow
structured validation responses suitable for client-side consumption rather than
direct strings to be displayed.
