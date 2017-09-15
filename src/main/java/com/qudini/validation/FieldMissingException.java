package com.qudini.validation;

import javax.annotation.Nonnull;

import static java.lang.String.format;

public final class FieldMissingException extends RuntimeException {
    public FieldMissingException(@Nonnull String fieldName) {
        super(format("field \"%s\" is missing", fieldName));
    }
}
