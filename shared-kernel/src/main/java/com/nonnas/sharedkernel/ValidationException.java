package com.nonnas.sharedkernel;

/**
 * Thrown when input fails validation against domain invariants
 * (e.g. negative quantity, blank required field, malformed CNPJ).
 *
 * <p>Maps to HTTP 400 Bad Request in the global exception handler.
 */
public non-sealed class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(ErrorCode.VALIDATION_FAILED, message, cause);
    }
}
