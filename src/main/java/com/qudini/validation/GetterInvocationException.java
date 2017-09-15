package com.qudini.validation;

import java.lang.reflect.Method;

import static java.lang.String.format;

public final class GetterInvocationException extends RuntimeException {
    GetterInvocationException(Method failingGetter, Exception cause) {
        super(format("a getter failed to invoke: %s", failingGetter), cause);
    }
}
