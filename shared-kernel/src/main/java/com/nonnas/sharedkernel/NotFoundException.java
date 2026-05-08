package com.nonnas.sharedkernel;

/**
 * Thrown when a referenced entity does not exist in the current persistence
 * context. Maps to HTTP 404 in the global exception handler.
 */
public non-sealed class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException(String entity, Object id) {
        super(ErrorCode.NOT_FOUND, "%s não encontrado: %s".formatted(entity, id));
    }
}
