package com.nonnas.sharedkernel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainExceptionTest {

    @Test
    @DisplayName("ValidationException carrega ErrorCode.VALIDATION_FAILED")
    void validationException() {
        ValidationException ex = new ValidationException("nome obrigatório");
        assertThat(ex.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(ex.getMessage()).isEqualTo("nome obrigatório");
    }

    @Test
    @DisplayName("ValidationException com cause encadeia exceção")
    void validationExceptionWithCause() {
        Throwable cause = new RuntimeException("origem");
        ValidationException ex = new ValidationException("falha", cause);
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("BusinessRuleException default usa ErrorCode.BUSINESS_RULE_VIOLATED")
    void businessRuleExceptionDefault() {
        BusinessRuleException ex = new BusinessRuleException("saldo insuficiente");
        assertThat(ex.code()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATED);
    }

    @Test
    @DisplayName("BusinessRuleException aceita ErrorCode customizado")
    void businessRuleExceptionWithCustomCode() {
        BusinessRuleException ex = new BusinessRuleException(ErrorCode.CONFLICT, "estado inválido");
        assertThat(ex.code()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("estado inválido");
    }

    @Test
    @DisplayName("BusinessRuleException com cause encadeia exceção")
    void businessRuleExceptionWithCause() {
        Throwable cause = new RuntimeException("origem");
        BusinessRuleException ex = new BusinessRuleException("falhou", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("NotFoundException carrega ErrorCode.NOT_FOUND")
    void notFoundException() {
        NotFoundException ex = new NotFoundException("Insumo", "abc-123");
        assertThat(ex.code()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getMessage()).contains("Insumo").contains("abc-123");
    }

    @Test
    @DisplayName("NotFoundException com mensagem customizada")
    void notFoundExceptionCustom() {
        NotFoundException ex = new NotFoundException("alvo customizado");
        assertThat(ex.getMessage()).isEqualTo("alvo customizado");
        assertThat(ex.code()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("DomainException é a raiz hierárquica")
    void hierarchy() {
        DomainException ex = new ValidationException("x");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("código nulo é rejeitado no construtor base")
    void nullCodeRejected() {
        assertThatThrownBy(() -> new BusinessRuleException(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }
}
