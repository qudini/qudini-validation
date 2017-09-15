package com.qudini.validation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

@ParametersAreNonnullByDefault
public final class FailedField {
    private final String field;
    private final Set<String> failures;

    FailedField(String field, Set<String> failures) {
        this.field = field;
        this.failures = unmodifiableSet(failures);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FailedField that = (FailedField) o;

        if (!getField().equals(that.getField())) return false;
        return getFailures().equals(that.getFailures());
    }

    @Override
    public int hashCode() {
        int result = getField().hashCode();
        result = 31 * result + getFailures().hashCode();
        return result;
    }

    public String getField() {
        return field;
    }

    public Set<String> getFailures() {
        return failures;
    }
}
