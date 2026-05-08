package com.nonnas.sharedkernel;

/**
 * Thrown when a business invariant is violated (e.g. attempt to receive a
 * transferência that is not in EM_TRANSITO state, attempt to use a lote
 * vencido in a venda).
 *
 * <p>Maps to HTTP 409 Conflict by default, but may carry alternative codes
 * via the two-argument constructor.
 */
public non-sealed class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATED, message);
    }

    public BusinessRuleException(ErrorCode code, String message) {
        super(code, message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(ErrorCode.BUSINESS_RULE_VIOLATED, message, cause);
    }
}
