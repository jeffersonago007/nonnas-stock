package com.nonnas.sharedkernel;

import java.util.Objects;

/**
 * Root of all domain exceptions. Sealed to {@link ValidationException},
 * {@link BusinessRuleException} and {@link NotFoundException}; the three
 * permitted subclasses are themselves {@code non-sealed} so that modules
 * can extend them with concrete business cases (e.g.
 * {@code LoteVencidoException extends BusinessRuleException}).
 *
 * <p>Every domain exception carries an {@link ErrorCode} so the global
 * exception handler (T09) can translate it to a stable HTTP status and
 * Problem Details response without inspecting the exception type.
 */
public abstract sealed class DomainException extends RuntimeException
        permits ValidationException, BusinessRuleException, NotFoundException {

    private final ErrorCode code;

    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    protected DomainException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public final ErrorCode code() {
        return code;
    }
}
