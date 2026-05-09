package com.nonnas.operations.interfaces.rest;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.DomainException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.nonnas.operations")
public class OperationsExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        return p(HttpStatus.BAD_REQUEST, ex.code(), "Dados inválidos", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return p(HttpStatus.NOT_FOUND, ex.code(), "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusiness(BusinessRuleException ex) {
        HttpStatus status = switch (ex.code()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return p(status, ex.code(), "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        return p(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), "Erro de domínio", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = p(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Falha de validação", "Campos inválidos");
        pd.setProperty("errors", fieldErrors);
        return pd;
    }

    private ProblemDetail p(HttpStatus s, ErrorCode c, String t, String d) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(s, d);
        pd.setTitle(t);
        pd.setProperty("code", c.name());
        return pd;
    }
}
