package com.nonnas.sharedkernel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    @Test
    @DisplayName("Result.success carrega o valor e expõe predicados")
    void successFlags() {
        Result<String> r = Result.success("ok");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.isFailure()).isFalse();
        assertThat(r.getOrThrow()).isEqualTo("ok");
        assertThat(r.toOptional()).contains("ok");
    }

    @Test
    @DisplayName("Result.success aceita valor nulo e produz Optional.empty")
    void successWithNull() {
        Result<String> r = Result.success(null);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.toOptional()).isEmpty();
        assertThat(r.getOrThrow()).isNull();
    }

    @Test
    @DisplayName("Result.failure carrega code e mensagem")
    void failureCarriesCodeAndMessage() {
        Result<String> r = Result.failure(ErrorCode.VALIDATION_FAILED, "campo obrigatório");
        assertThat(r.isFailure()).isTrue();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(r.errorMessage()).isEqualTo("campo obrigatório");
        assertThat(r.toOptional()).isEmpty();
    }

    @Test
    @DisplayName("Failure.getOrThrow lança BusinessRuleException com code e message")
    void failureGetOrThrow() {
        Result<String> r = Result.failure(ErrorCode.NOT_FOUND, "registro inexistente");
        assertThatThrownBy(r::getOrThrow)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("registro inexistente")
                .extracting(t -> ((DomainException) t).code())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("Success.errorCode lança IllegalStateException")
    void successErrorCodeThrows() {
        Result<String> r = Result.success("ok");
        assertThatThrownBy(r::errorCode).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(r::errorMessage).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("map em Success aplica a função")
    void mapOnSuccess() {
        Result<Integer> r = Result.<String>success("42").map(Integer::parseInt);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getOrThrow()).isEqualTo(42);
    }

    @Test
    @DisplayName("map em Failure mantém o erro inalterado")
    void mapOnFailure() {
        Result<String> failed = Result.failure(ErrorCode.VALIDATION_FAILED, "ruim");
        Result<Integer> mapped = failed.map(Integer::parseInt);
        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(mapped.errorMessage()).isEqualTo("ruim");
    }

    @Test
    @DisplayName("flatMap em Success encadeia computação")
    void flatMapOnSuccess() {
        Result<Integer> r = Result.<String>success("10")
                .flatMap(s -> Result.success(Integer.parseInt(s) * 2));
        assertThat(r.getOrThrow()).isEqualTo(20);
    }

    @Test
    @DisplayName("flatMap em Success pode produzir Failure")
    void flatMapSuccessToFailure() {
        Result<Integer> r = Result.<String>success("x")
                .flatMap(s -> Result.failure(ErrorCode.VALIDATION_FAILED, "não numérico"));
        assertThat(r.isFailure()).isTrue();
    }

    @Test
    @DisplayName("flatMap em Failure mantém o erro original")
    void flatMapOnFailure() {
        Result<String> failed = Result.failure(ErrorCode.NOT_FOUND, "miss");
        Result<Integer> mapped = failed.flatMap(s -> Result.success(1));
        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("Failure rejeita code nulo")
    void failureRejectsNullCode() {
        assertThatThrownBy(() -> new Result.Failure<>(null, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Failure rejeita message nula")
    void failureRejectsNullMessage() {
        assertThatThrownBy(() -> new Result.Failure<>(ErrorCode.UNEXPECTED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Success.map rejeita mapper nulo")
    void successMapRejectsNullMapper() {
        Result<String> r = Result.success("x");
        assertThatThrownBy(() -> r.map(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Success.flatMap rejeita mapper nulo")
    void successFlatMapRejectsNullMapper() {
        Result<String> r = Result.success("x");
        assertThatThrownBy(() -> r.flatMap(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Failure.map rejeita mapper nulo")
    void failureMapRejectsNullMapper() {
        Result<String> r = Result.failure(ErrorCode.UNEXPECTED, "x");
        assertThatThrownBy(() -> r.map(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Failure.flatMap rejeita mapper nulo")
    void failureFlatMapRejectsNullMapper() {
        Result<String> r = Result.failure(ErrorCode.UNEXPECTED, "x");
        assertThatThrownBy(() -> r.flatMap(null)).isInstanceOf(NullPointerException.class);
    }
}
