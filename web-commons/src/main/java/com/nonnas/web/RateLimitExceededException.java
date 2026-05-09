package com.nonnas.web;

/**
 * Sinaliza que a quantidade de requisições excedeu o limite configurado.
 * Mapeada para HTTP 429 pelo {@link GlobalExceptionHandler}.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Limite de requisições excedido. Tente novamente em "
                + retryAfterSeconds + " segundo(s).");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
