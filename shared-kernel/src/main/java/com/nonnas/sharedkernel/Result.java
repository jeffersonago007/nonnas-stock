package com.nonnas.sharedkernel;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Either-type for use case returns where failure is an expected outcome
 * (validation that the caller must inspect) rather than an exception.
 *
 * <p>Prefer {@code Result} over throwing when the failure is part of the
 * normal control flow — e.g. "esse insumo já existe", "saldo insuficiente".
 * Reserve domain exceptions for invariants that must abort the transaction.
 *
 * <p>Sealed with two records, {@link Success} and {@link Failure}, to enable
 * exhaustive pattern matching:
 *
 * <pre>{@code
 * return switch (result) {
 *     case Result.Success<Insumo> s -> ResponseEntity.ok(s.value());
 *     case Result.Failure<Insumo> f -> ResponseEntity.badRequest().body(f.message());
 * };
 * }</pre>
 *
 * @param <T> the success value type
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();

    boolean isFailure();

    /**
     * Returns the success value or throws a {@link BusinessRuleException}
     * carrying the failure code and message.
     */
    T getOrThrow();

    /**
     * Wraps the success value in {@link Optional}, or returns empty on
     * failure. Note: a {@code Success} with a {@code null} payload yields
     * {@link Optional#empty()}.
     */
    Optional<T> toOptional();

    ErrorCode errorCode();

    String errorMessage();

    <R> Result<R> map(Function<? super T, ? extends R> mapper);

    <R> Result<R> flatMap(Function<? super T, Result<R>> mapper);

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(ErrorCode code, String message) {
        return new Failure<>(code, message);
    }

    record Success<T>(T value) implements Result<T> {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public T getOrThrow() {
            return value;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }

        @Override
        public ErrorCode errorCode() {
            throw new IllegalStateException("Success has no error code");
        }

        @Override
        public String errorMessage() {
            throw new IllegalStateException("Success has no error message");
        }

        @Override
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return mapper.apply(value);
        }
    }

    record Failure<T>(ErrorCode code, String message) implements Result<T> {

        public Failure {
            Objects.requireNonNull(code, "code must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public T getOrThrow() {
            throw new BusinessRuleException(code, message);
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public ErrorCode errorCode() {
            return code;
        }

        @Override
        public String errorMessage() {
            return message;
        }

        @Override
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return new Failure<>(code, message);
        }

        @Override
        public <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return new Failure<>(code, message);
        }
    }
}
