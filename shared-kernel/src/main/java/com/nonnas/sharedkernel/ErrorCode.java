package com.nonnas.sharedkernel;

/**
 * Stable error codes used to map domain exceptions to RFC 7807 Problem
 * Details responses (T09) and to drive i18n keys when a translation layer
 * is added later.
 *
 * <p>Codes are intentionally coarse-grained at the kernel level. Specific
 * scenarios (e.g. "lote vencido") are conveyed via the exception message
 * and module-level subclasses, not by adding granular codes here.
 */
public enum ErrorCode {
    VALIDATION_FAILED,
    BUSINESS_RULE_VIOLATED,
    NOT_FOUND,
    UNAUTHORIZED,
    FORBIDDEN,
    CONFLICT,
    UNEXPECTED
}
